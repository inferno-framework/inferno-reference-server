package org.mitre.fhir.authorization;

import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hl7.fhir.r4.model.Bundle;
import org.mitre.fhir.authorization.exception.InvalidBearerTokenException;
import org.mitre.fhir.authorization.exception.InvalidScopesException;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.authorization.token.TokenNotFoundException;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.springframework.web.util.UriComponentsBuilder;

public class FakeOauth2AuthorizationInterceptorAdaptor extends InterceptorAdapter {

  private static final String CONFORMANCE_PATH = "/metadata";
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";

  private final DaoRegistry registry;

  public FakeOauth2AuthorizationInterceptorAdaptor(DaoRegistry registry) {
    this.registry = registry;
  }

  @Override
  public boolean incomingRequestPostProcessed(RequestDetails requestDetails,
      HttpServletRequest request, HttpServletResponse response) {

    // exempt the capability statement from requiring the token
    if (CONFORMANCE_PATH.equals(request.getPathInfo())) {
      return true;
    }

    List<String> scopesArray;
    TokenManager tokenManager = TokenManager.getInstance();

    String bearerToken = requestDetails.getHeader("Authorization");

    if (bearerToken == null) {
      throw new InvalidBearerTokenException(bearerToken);
    }

    bearerToken = bearerToken.replaceFirst(BEARER_TOKEN_PREFIX, "");

    try {
      tokenManager.authenticateBearerToken(bearerToken);
      scopesArray = tokenManager.getToken(bearerToken).getScopes();
    } catch (TokenNotFoundException e) {
      throw new InvalidBearerTokenException(bearerToken);
    }

    // scope format is slightly different between SMART app launch 1 and 2:
    // in 1: ( 'patient' | 'user' ) '/' ( fhir-resource | '*' ) '.' ( 'read' |
    // 'write' | '*' )`
    // https://hl7.org/fhir/smart-app-launch/1.0.0/scopes-and-launch-context/index.html

    // in 2: <patient|user|system> / <fhir-resource>. <c | r | u | d |s>
    // [?param=value[&param2=value2...]]
    // http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context.html#fhir-resource-scope-syntax


    String resource = requestDetails.getResourceName();
    if (resource == null || resource.equals("Patient")) {
      // TODO: this maintains the previous behavior, but review to see if this still
      // makes sense
      return true;
    }

    List<Scope> grantedScopes = scopesArray.stream().map(s -> Scope.fromString(s)).toList();

    boolean anyScopeApplies = false;
    Map<String, String> parametersToAdd = new HashMap<>();
    for (Scope s : grantedScopes) {
      // note: order is important here, we do not want to short circuit
      anyScopeApplies = s.apply(requestDetails, parametersToAdd, registry, request) || anyScopeApplies;
    }

    if (!anyScopeApplies) {
      throw new InvalidScopesException(resource);
    }

    Map<String, String[]> requestParams = requestDetails.getParameters();
    for (String key : parametersToAdd.keySet()) {
      // requestDetails.addParameters overrides anything that is already there, so we
      // need to be careful to actually add
      // request parameters can enable AND and OR logic:
      // Single: ?category=survey
      // AND (vital-signs & laboratory): ?category=vital-signs&category=laboratory
      // That's two params with individual values
      // OR (vital-signs | laboratory): ?category=vital-signs,laboratory
      // That's one param with the values joined by a comma
      // COMPLEX (laboratory & (vital-signs | imaging)):
      // ?category=laboratory&category=vital-signs,imaging
      // Ex, if user searches by laboratory but their scopes are vital-signs and
      // imaging scopes
      // These can't be nested arbitrarily deep but I don't think we have to.
      // So to apply the scopes to this request, we add an additional param (AND) with
      // the scopes values joined (OR)

      // TODO: this impl could be improved further by not adding new params if the
      // existing ones already cover it
      // eg, if the user searched for category=imaging, don't add another param for
      // category=vital-signs,imaging as it would be redundant
      String[] existingValues = requestParams.get(key);
      if (existingValues == null) {
        existingValues = new String[0];
      }

      String[] newValues = new String[existingValues.length + 1];
      System.arraycopy(existingValues, 0, newValues, 0, existingValues.length);

      newValues[existingValues.length] = parametersToAdd.get(key);

      requestDetails.addParameter(key, newValues);
    }
        
    return true;
  }

  public static class Scope {
    String rawValue;
    boolean isWildcardResource = false;
    String resourceType;

    boolean create = false;
    boolean read = false;
    boolean update = false;
    boolean delete = false;
    boolean search = false;

    org.springframework.util.MultiValueMap<String, String> parameters;

    static final Pattern SCOPE_REGEX = Pattern
        .compile("([a-z]+)/(\\*|\\p{Alpha}+)\\.(\\*|read|write|c?r?u?d?s?)(\\?.*)?");

    public static Scope fromString(String scopeStr) {
      Scope s = new Scope();

      Matcher m = SCOPE_REGEX.matcher(scopeStr);

      if (m.matches()) {
        // String level = m.group(1); // patient/user/system. not currently used

        s.resourceType = m.group(2);
        if ("*".equals(s.resourceType))
          s.isWildcardResource = true;

        String permissions = m.group(3);

        if (permissions != null) {
          if (permissions.equals("*")) {
            s.create = true;
            s.read = true;
            s.update = true;
            s.delete = true;
            s.search = true;
          } else if (permissions.equals("read")) {
            s.read = true;
            s.search = true;
          } else if (permissions.equals("write")) {
            s.create = true;
            s.update = true;
            s.delete = true;
          } else {
            if (permissions.contains("c"))
              s.create = true;
            if (permissions.contains("r"))
              s.read = true;
            if (permissions.contains("u"))
              s.update = true;
            if (permissions.contains("d"))
              s.delete = true;
            if (permissions.contains("s"))
              s.search = true;
          }
        }

        String params = m.group(4);
        if (params != null) {
          s.parameters = UriComponentsBuilder.fromUriString(params).build().getQueryParams();
        }
      }

      return s;
    }

    public boolean apply(RequestDetails requestDetails, Map<String, String> parametersToAdd, DaoRegistry registry, HttpServletRequest request) {
      String resource = requestDetails.getResourceName();
      if (!isWildcardResource && !resource.equals(resourceType)) {
        // this scope is not relevant to the current request
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
        if (this.read && this.parameters != null) {
          // if this scope is granular, do a search with this scopes's params and the current resource ID to see if this read is allowed
          // if nothing is returned, ie, either the params don't match the resource or the resource with that ID doesn't exist, return not allowed
          
          
//          IFhirResourceDao<?> dao = registry.getResourceDao(resource);
//          SearchParameterMap searchParams = new SearchParameterMap();
//          searchParams.add("_id", new ca.uhn.fhir.rest.param.StringParam(requestDetails.getId().getValueAsString()));
//          searchParams.setSummaryMode(SummaryEnum.COUNT);
//          List results = dao.searchForResourceIds(searchParams, requestDetails);
          

          // this is a terrible hack
          try {
            IGenericClient client = FhirReferenceServerUtils.FHIR_CONTEXT_R4.newRestfulGenericClient(FhirReferenceServerUtils.getFhirServerBaseUrl(request));
            Bundle results = (Bundle) client.search().byUrl(resource + "?_summary=count&_id=" + requestDetails.getId().getValueAsString()).withAdditionalHeader("Authorization", request.getHeader("Authorization")).execute();
            return results.getTotal() > 0;
          } catch (Exception e) {
            e.printStackTrace();
            return false;
          }
        }
        return this.read;


      case SEARCH_TYPE:
        if (this.search && this.parameters != null) {
          // if there are granular scopes, apply those here
          // eg requestDetails.addParameter("category", new String[] {"http://terminology.hl7.org/CodeSystem/observation-category|social-history"});
          // unfortunately we can't add directly to the request in case another scope is also relevant
          // we need to OR all relevant scopes together
          
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

          return true;
        }
        return this.search;

      default:
        // maintains the previous behavior
        return true;
      
      }
    }
  }
}
