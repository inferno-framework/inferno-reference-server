package gov.onc.authorization;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;

public class FakeOauth2AuthorizationInterceptorAdaptor extends InterceptorAdapter{
	
	private static String CONFORMANCE_PATH = "/metadata";
	
	@Override
	public boolean incomingRequestPostProcessed(RequestDetails requestDetails, HttpServletRequest request, HttpServletResponse response )
	{
		request.getPathInfo();
					
		if (CONFORMANCE_PATH.equals(request.getPathInfo()))
		{
			return true;
		}
		
		String bearerToken = requestDetails.getHeader("Authorization");
	     
		return isBearerTokenValid(bearerToken);
		
	}
	
	public boolean isBearerTokenValid(String bearerToken)
	{
		return "SAMPLE_TOKEN".equals(bearerToken);
	}
}
