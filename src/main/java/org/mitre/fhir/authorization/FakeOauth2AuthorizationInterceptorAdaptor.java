package org.mitre.fhir.authorization;

import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import org.mitre.fhir.authorization.exception.InvalidBearerTokenException;

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

		if (!isBearerTokenValid(bearerToken)) {
			Enumeration<String> headers = request.getHeaderNames();
			while (headers.hasMoreElements()) {
				String currentHeader = headers.nextElement();
				System.out.println("Header Name is " + currentHeader + " , and Header Value is "
						+ request.getHeader(currentHeader));
			}

			throw new InvalidBearerTokenException(bearerToken);
		}

		return true;

	}

	private boolean isBearerTokenValid(String bearerToken) {
		return (BEARER_TOKEN_PREFIX + EXPECTED_BEARER_TOKEN).equals(bearerToken);
	}

}
