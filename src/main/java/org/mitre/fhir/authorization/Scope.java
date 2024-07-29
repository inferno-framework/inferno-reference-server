package org.mitre.fhir.authorization;

import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * The Scope class represents a single SMART OAuth 2.0 scope.
 * This class is designed to answer the question "does this scope allow access to a given request?"
 * and apply constraints to the request as appropriate.
 * The main entrypoint is Scope.fromString(String).
 * Scopes that the server knows are invalid and cannot be processed should not be returned,
 * however anything "unknown" will still be returned, to allow maximum flexibility.
 */
public class Scope implements Cloneable {
  String rawValue;
  boolean isWildcardResource = false;

  int version;
  String level;
  String resourceType;

  boolean create;
  boolean read;
  boolean update;
  boolean delete;
  boolean search;

  MultiValueMap<String, String> parameters;

  /**
   * Regex representing the SMART 1.0 and 2.0 scope syntax.
   */
  /* (Non-javadoc to prevent parse errors)
   * In 1: 
   * ( 'patient' | 'user' ) '/' ( fhir-resource | '*' ) '.' ( 'read' | 'write' | '*' )`
   * https://hl7.org/fhir/smart-app-launch/1.0.0/scopes-and-launch-context/index.html
   * 
   * In 2: 
   * <patient|user|system> / <fhir-resource>. <c | r | u | d |s> [?param=value[&param2=value2...]]
   * http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context.html#fhir-resource-scope-syntax
   */
  private static final Pattern SCOPE_REGEX = Pattern
      .compile("([a-z]+)/(\\*|\\p{Alpha}+)\\.(\\*|read|write|c?r?u?d?s?)(\\?.*)?");

  /**
   * The map of resourceType -> search parameters that are allowed in granular scopes.
   */
  private static Map<String, Set<String>> allKnownSearchParams = new HashMap<>();
  
  /**
   * Set the given map of search parameters as the new set of search parameters.
   * (Replace, not add).
   */
  public static void registerSearchParams(Map<String, Set<String>> allSearchParams) {
    allKnownSearchParams = new HashMap<>(allSearchParams);
  }

  /**
   * Get a Scope for the given scope string.
   * If the server recognizes the string as invalid, null will be returned instead.
   */
  public static Scope fromString(String scopeStr) {
    Scope s = new Scope(scopeStr);
    if (!s.isValid()) {
      s = null;
    }
    return s;
  }
  
  /**
   * Package-private constructor - all access (other than unit tests) should be
   * through the Scope.fromString() method.
   */
  Scope(String scopeStr) {
    this.rawValue = scopeStr;
    
    Matcher m = SCOPE_REGEX.matcher(scopeStr);

    if (m.matches()) {
      this.level = m.group(1); // patient/user/system

      this.resourceType = m.group(2);
      if ("*".equals(this.resourceType)) {
        this.isWildcardResource = true;
      }

      String permissions = m.group(3);

      if (permissions != null) {
        if (permissions.equals("*")) {
          this.version = 1;
          this.create = true;
          this.read = true;
          this.update = true;
          this.delete = true;
          this.search = true;
        } else if (permissions.equals("read")) {
          this.version = 1;
          this.read = true;
          this.search = true;
          
          this.create = false;
          this.update = false;
          this.delete = false;
        } else if (permissions.equals("write")) {
          this.version = 1;
          this.create = true;
          this.update = true;
          this.delete = true;
          
          this.read = false;
          this.search = false;
        } else {
          this.version = 2;
          this.create = permissions.contains("c");
          this.read = permissions.contains("r");
          this.update = permissions.contains("u");
          this.delete = permissions.contains("d");
          this.search = permissions.contains("s");
        }
      }

      String params = m.group(4);
      if (params != null) {
        this.parameters = UriComponentsBuilder.fromUriString(params).build().getQueryParams();
      }
    }
  }

  /**
   * Determines whether the current scope allows access to the given request,
   * and adds the current scope's restrictions to a map of parameters.
   * 
   * @param requestDetails FHIR request details
   * @param parametersToAdd Map to add search parameters that a granular scope applies.
   * @param canAccessResource
   *          Function that determines whether a given resource (by type/ID) can be accessed.
   *          This function will be called if the scope is broadly applicable to the request,
   *          and a more fine-grained approach is necessary to know whether the scopes
   *          allow access to the specific resource being requested.
   * @param isShortCircuitAllowed
   *          Whether this scope is to allowed short-circuit any resource-intensive steps
   *          (ie, if a previous scope already granted access)
   * @return True if the scope enables the given request,
   *         false if not relevant or if the scope does not enable the given request.
   */
  public boolean apply(RequestDetails requestDetails, Map<String, String> parametersToAdd,
      BiPredicate<String, String> canAccessResource, boolean isShortCircuitAllowed) {
    String resource = requestDetails.getResourceName();
    if (!isWildcardResource && !resource.equals(resourceType)) {
      // This scope is not relevant to the current request
      return false;
    }
    
    RestOperationTypeEnum operation = requestDetails.getRestOperationType();
    switch (operation) {
      case CREATE:
        return this.create;
      case UPDATE:
        return this.update;
      case DELETE:
        return this.delete;
  
      case READ:
        if (this.read && this.parameters != null && !isShortCircuitAllowed) {
          // If this scope is granular, we need to see if this scope allows this particular read.
          // We can short-circuit and skip the check if a previous scope already granted access.
          return canAccessResource.test(resource, requestDetails.getId().getValueAsString());
        }
        return this.read;
  
      case SEARCH_TYPE:
        if (this.search && this.parameters != null) {
          // If there are granular scopes, apply those here.
          // e.g.: requestDetails.addParameter("category", new String[] {"social-history"});
          // But we can't add directly to the request, in case another scope is also relevant.
          // So we need to OR all relevant values together by joining with commas.
          // The parametersToAdd arg is re-used as we iterate through Scopes.
          for (String key : this.parameters.keySet()) {
            for (String toAdd : this.parameters.get(key)) {
              String value = parametersToAdd.get(key);
              if (value == null) {
                value = toAdd;
              } else {
                value = value + "," + toAdd;
              }
              parametersToAdd.put(key, value);
            }
          }
        }
        return this.search;
  
      default:
        // maintains the previous behavior
        return true;
    }
  }

  /**
   * Return a new Scope that represents the same privileges as this Scope,
   * upgraded to SMART v2.0. If this scope is already v2.0, or isn't recognized
   * as a SMART scope, this scope is returned.
   */
  public Scope asVersion2() {
    if (version != 1) {
      return this;
    }

    try {
      Scope thisV2 = this.clone();
      thisV2.version = 2;
      // Null out the rawValue and call toString to re-create it.
      thisV2.rawValue = null;
      thisV2.toString();
      return thisV2;
    } catch (CloneNotSupportedException e) {
      // Should never happen, but it's a checked exception so we catch it
      throw new IllegalStateException(e);
    }
  }

  @Override
  protected Scope clone() throws CloneNotSupportedException {
    return (Scope) super.clone();
  }

  /**
   * Determine whether this Scope is valid.
   * To maximize flexibility, "invalid" here means anything that is known to be incorrect
   * or not processable. Anything else or "unknown" is ok and valid, and should return true.
   */
  boolean isValid() {
    // For now the only thing to check is granular scope parameters.
    if (parameters != null && !parameters.isEmpty()) {
      if (isWildcardResource) {
        // Granular scopes not allowed on wildcard resources.
        return false;
      }
      
      Set<String> resourceParams = allKnownSearchParams.get(this.resourceType);
      for (String param : parameters.keySet()) {
        // Only check if the keys are valid search parameters for the given resource.
        // Invalid search parameter names, such as `Condition?not_a_param=123`, would cause errors.
        // It doesn't matter if the value isn't actually possible.
        if (!resourceParams.contains(param)) {
          return false;
        }
      }
    }
    return true;
  }

  @Override
  public String toString() {
    if (this.rawValue != null) {
      return this.rawValue;
    }

    StringBuilder scopeString = new StringBuilder();

    // Both versions start with level/resourceType.
    scopeString.append(this.level).append('/').append(this.resourceType).append('.');

    if (this.version == 1) {
      if (this.read && this.create) {
        scopeString.append('*');
      } else if (this.read) {
        scopeString.append("read");
      } else if (this.create) {
        scopeString.append("write");
      }
    } else if (this.version == 2) {
      if (this.create) {
        scopeString.append('c');
      }
      if (this.read) {
        scopeString.append('r');
      }
      if (this.update) {
        scopeString.append('u');
      }
      if (this.delete) {
        scopeString.append('d');
      }
      if (this.search) {
        scopeString.append('s');
      }

      if (this.parameters != null && !this.parameters.isEmpty()) {
        boolean first = true;
        for (String key : this.parameters.keySet()) {
          for (String value : this.parameters.get(key)) {
            scopeString.append(first ? '?' : '&').append(key).append('=').append(value);
            first = false;
          }
        }
      }
    } else {
      throw new IllegalStateException("Unexpected version '" + version + "' in Scope.toString");
    }

    this.rawValue = scopeString.toString();
    return this.rawValue;
  }
}