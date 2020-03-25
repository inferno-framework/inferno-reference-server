package org.mitre.fhir.authorization;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import org.mitre.fhir.authorization.exception.InvalidBearerTokenException;
import org.mitre.fhir.authorization.exception.InvalidScopesException;
import org.postgresql.util.Base64;

public class FakeOauth2AuthorizationInterceptorAdaptor extends InterceptorAdapter {

	private static final String CONFORMANCE_PATH = "/metadata";
	private static final String EXPECTED_BEARER_TOKEN = "SAMPLE_ACCESS_TOKEN";
	private static final String BEARER_TOKEN_PREFIX = "Bearer ";

	@Override
	public boolean incomingRequestPostProcessed(RequestDetails requestDetails, HttpServletRequest request,
			HttpServletResponse response) {
		
		// exempt the capability statement from requiring the token
		if (CONFORMANCE_PATH.equals(request.getPathInfo())) {
			return true;
		}

		String bearerToken = requestDetails.getHeader("Authorization");
		
		if (bearerToken == null)
		{
			throw new InvalidBearerTokenException(bearerToken);
		}
		
		bearerToken = bearerToken.replaceFirst(BEARER_TOKEN_PREFIX, "");
				
		String[] splitBearerTokenParts = bearerToken.split("\\.");
		
		if (splitBearerTokenParts.length != 2)
		{
			throw new InvalidBearerTokenException(bearerToken);
		}
		
		String actualBearerToken = splitBearerTokenParts[0];
		

		if (!isBearerTokenValid(actualBearerToken)) {
			throw new InvalidBearerTokenException(bearerToken);
		}
		
		String encodedScopes = splitBearerTokenParts[1];

		String scopes = new String(Base64.decode(encodedScopes));
		
		
		List<String> scopesArray = Arrays.asList(scopes.split(" "));
		List<String> validResources = new ArrayList<String>();


		for (String currentScope : scopesArray)
		{	
			//strip off user or patient part of scope
			String[] scopeParts = currentScope.split("/");
			if (scopeParts.length == 2)
			{
				//for now strip off operation part of scope
				
				String scopeAfterSlash = scopeParts[1];
				String[] scopeAfterSlashParts = scopeAfterSlash.split("\\.");
				
				
				if (scopeAfterSlashParts.length == 2)
				{
					validResources.add(scopeAfterSlashParts[0]);
				}
				
				else
				{
					validResources.add(scopeAfterSlash);
				}
			}	
			
		}				
		
		//break bear token into scopes
		String resource = requestDetails.getResourceName();
		
		if (!validResources.contains("*") && !validResources.contains(resource) && !("Patient".equals(resource)))
		{
			throw new InvalidScopesException(resource);
		}

		return true;

	}

	private boolean isBearerTokenValid(String bearerToken) {
		return EXPECTED_BEARER_TOKEN.equals(bearerToken);
	}

}
