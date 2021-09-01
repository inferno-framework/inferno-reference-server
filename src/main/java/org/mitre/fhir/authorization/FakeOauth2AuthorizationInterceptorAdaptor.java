
package org.mitre.fhir.authorization;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mitre.fhir.authorization.exception.InvalidBearerTokenException;
import org.mitre.fhir.authorization.exception.InvalidScopesException;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.authorization.token.TokenNotFoundException;

public class FakeOauth2AuthorizationInterceptorAdaptor extends InterceptorAdapter {

  private static final String CONFORMANCE_PATH = "/metadata";
  private static final String BEARER_TOKEN_PREFIX = "Bearer ";

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


    } catch (TokenNotFoundException e) {
      throw new InvalidBearerTokenException(bearerToken);
    }

    try {
      scopesArray = tokenManager.getToken(bearerToken).getScopes();
    } catch (TokenNotFoundException tokenNotFoundException) {
      throw new InvalidBearerTokenException(bearerToken);

    }

    List<String> grantedResources = new ArrayList<String>();

    for (String currentScope : scopesArray) {
      // strip off user or patient part of scope
      String[] scopeParts = currentScope.split("/");
      if (scopeParts.length == 2) {
        // for now strip off operation part of scope

        String scopeAfterSlash = scopeParts[1];
        String[] scopeAfterSlashParts = scopeAfterSlash.split("\\.");

        if (scopeAfterSlashParts.length == 2) {
          grantedResources.add(scopeAfterSlashParts[0]);
        } else {
          grantedResources.add(scopeAfterSlash);
        }
      }

    }

    String resource = requestDetails.getResourceName();

    if (!grantedResources.contains("*") && !grantedResources.contains(resource)
        && !("Patient".equals(resource))) {
      if (resource != null) {
        throw new InvalidScopesException(resource);
      }
    }
    
    /*
    System.out.println();
    System.out.println();
    System.out.println();
    System.out.println("======");
    System.out.println("The PathIfno is " + request.getPathInfo());
    System.out.println("The Method is " + request.getMethod());
    System.out.println("======");
    System.out.println();
    System.out.println();
    System.out.println();
    
    if (!request.getPathInfo().startsWith("/Patient") && !request.getPathInfo().startsWith("/Encounter"))
    {
      throw new InvalidBearerTokenException(request.getPathInfo());
    }

    
    //check for bulk delete
    if (request.getPathInfo().equals("/$export-poll-status") && request.getMethod().equals("DELETE"))
    {
      
      
      //update request and request details to route to different method
      requestDetails.setOperation("GET");
      requestDetails.setRequestPath("/bulk-delete");
      
      throw new InvalidBearerTokenException("GOT HERE " + request.getPathInfo());

      
    }
    */

    return true;
  }
  
}
