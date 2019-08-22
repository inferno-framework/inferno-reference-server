package gov.onc.authorization;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;

public class FakeOauth2AuthorizationInterceptorAdaptor extends InterceptorAdapter{
	
	@Override
	public boolean incomingRequestPostProcessed(RequestDetails requestDetails, HttpServletRequest request, HttpServletResponse response )
	{
		String bearerToken = ""; //logic to extract bearer token from the security headers
		if (isBearerTokenValid(bearerToken))
		{
			return true;
		}
		return false;
	}
	
	public boolean isBearerTokenValid(String bearerToken)
	{
		return "SAMPLE_TOKEN".equals(bearerToken);
	}
}
