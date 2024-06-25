package org.mitre.fhir.authorization;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;

import org.hl7.fhir.r4.model.Bundle;
import org.mitre.fhir.authorization.exception.InvalidBearerTokenException;
import org.mitre.fhir.authorization.exception.InvalidScopesException;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.authorization.token.TokenNotFoundException;
import org.mitre.fhir.utils.FhirReferenceServerUtils;

public class FakeOauth2AuthorizationInterceptorAdaptor extends InterceptorAdapter {

  private static final String CONFORMANCE_PATH = "/metadata";
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";

  /**
   * Get a function with signature (resourceType, id) -> boolean that will determine whether
   * the resource in question can be accessed by the current user.
   */
  private BiPredicate<String, String> canAccessResourcePredicate(HttpServletRequest request) {
    return (resourceType, resourceID) -> {
      // This is frankly a hack.
      // We use the same authorization on a search by ID, and their scopes will apply to that request implicitly.
      // E.g. GET /Patient/123 --> GET /Patient?_id=123
      // If the search returns no results (either the scopes don't match what's on the resource, or the resource ID doesn't exist)
      // then we don't allow a read of that specific resource.

      // Ideally this might use an internal query to evaluate the resource against the query params,
      // but converting the scope into internal query params is nontrivial.
      try {
        IGenericClient client = FhirReferenceServerUtils.FHIR_CONTEXT_R4
            .newRestfulGenericClient(FhirReferenceServerUtils.getFhirServerBaseUrl(request));
        // Since we only care about count, add _summary=count to the query params here.
        // It should reduce the impact of this extra query.
        Bundle results = (Bundle) client.search()
            .byUrl(resourceType + "?_summary=count&_id=" + resourceID)
            .withAdditionalHeader("Authorization", request.getHeader("Authorization")).execute();
        return results.getTotal() > 0;
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }
    };
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

    String resource = requestDetails.getResourceName();
    if (resource == null || resource.equals("Patient")) {
      // TODO: this maintains the previous behavior, but does this make sense?
      return true;
    }

    List<Scope> grantedScopes = scopesArray.stream().map(s -> Scope.fromString(s)).toList();

    boolean anyScopeApplies = false;
    Map<String, String> parametersToAdd = new HashMap<>();
    for (Scope s : grantedScopes) {
      // Note: order is important here. The scope itself may short-circuit its processing if possible,
      // but we don't want to short-circuit and not apply a scope when a previous one grants access.
      // In the case of granular scopes, each scope is "additive" in terms of resources it grants access to,
      // so we always want to apply all granular scopes to a search.
      anyScopeApplies = s.apply(requestDetails, parametersToAdd, canAccessResourcePredicate(request), anyScopeApplies) || anyScopeApplies;
    }

    if (!anyScopeApplies) {
      throw new InvalidScopesException(resource);
    }

    Map<String, String[]> requestParams = requestDetails.getParameters();
    for (String key : parametersToAdd.keySet()) {
      // requestDetails.addParameters overrides anything that is already there, 
      // so we need to be careful to actually add.

      /* Request parameters can enable AND and OR logic:
          
          Single: ?category=survey
          
          AND (vital-signs & laboratory): ?category=vital-signs&category=laboratory
          - That's two params with individual values
          
          OR (vital-signs | laboratory): ?category=vital-signs,laboratory
          - That's one param with the values joined by a comma
           
          COMPLEX (laboratory & (vital-signs | imaging)):
          ?category=laboratory&category=vital-signs,imaging
          - Ex, if user searches by laboratory but their scopes are vital-signs and imaging scopes.
          - These can't be nested arbitrarily deep but I don't think we have to.
          
          
        So to apply the scopes to this request,
        we add an additional param (AND) with the scopes values joined (OR)
       */

      // TODO: this impl could be improved further by not adding new params if the
      // existing ones already cover it.
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
}
