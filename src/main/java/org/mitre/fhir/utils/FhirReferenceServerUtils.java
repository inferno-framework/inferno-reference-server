package org.mitre.fhir.utils;

import java.util.Base64;
import javax.servlet.http.HttpServletRequest;

public class FhirReferenceServerUtils {

	public static final String SAMPLE_CODE = "SAMPLE_CODE";

	public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	public static final String BEARER_TOKEN_PREFIX = "Bearer";
	public static final String FHIR_SERVER_PATH = "/r4";
	public static final String REFERENCE_SERVER_PATH = "/app";

	public static final String SAMPLE_PUBLIC_CLIENT_ID = "SAMPLE_PUBLIC_CLIENT_ID";
	public static final String SAMPLE_CONFIDENTIAL_CLIENT_ID = "SAMPLE_CONFIDENTIAL_CLIENT_ID";
	public static final String SAMPLE_CONFIDENTIAL_CLIENT_SECRET = "SAMPLE_CONFIDENTIAL_CLIENT_SECRET";

	public static final String DEFAULT_SCOPE = "launch/patient patient/*";

	private static final String HTTP = "http";
	private static final String HTTPS = "https";
	private static final int HTTP_DEFAULT_PORT = 80;
	private static final int HTTPS_DEFAULT_PORT = 443;

	public static String getServerBaseUrl(HttpServletRequest request) {
		String scheme = request.getScheme();
		int portNumber = request.getServerPort();
		String port = ":" + portNumber;

		// if default port, remove the port
		if ((HTTP.equals(scheme) && portNumber == HTTP_DEFAULT_PORT)
				|| (HTTPS.equals(scheme) && portNumber == HTTPS_DEFAULT_PORT)) {
			port = "";
		}

		String serverBaseUrl = request.getScheme() + "://" + request.getServerName() + port + "/reference-server";
		return serverBaseUrl;
	}

	public static String getFhirServerBaseUrl(HttpServletRequest request) {
		return getServerBaseUrl(request) + FHIR_SERVER_PATH;
	}

	public static String getSmartStyleUrl(HttpServletRequest request) {
		return getServerBaseUrl(request) + REFERENCE_SERVER_PATH + "/smart-style-url";
	}

	public static String createCode(String actualCode, String scopes, String patientId) {
		String encodedScope = Base64.getEncoder().encodeToString(scopes.getBytes());
		String encodedPatientId = Base64.getEncoder().encodeToString(patientId.getBytes());

		return actualCode + "." + encodedScope + "." + encodedPatientId;
	}

	public static String createAuthorizationHeaderValue(String accessToken, String scopes) {
		String encodedScopes = Base64.getEncoder().encodeToString(scopes.getBytes());
		return BEARER_TOKEN_PREFIX + " " + accessToken + "." + encodedScopes;

	}

}
