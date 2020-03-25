package org.mitre.fhir.utils;

import java.util.Base64;

import javax.servlet.http.HttpServletRequest;

public class FhirReferenceServerUtils {

	public static final String SAMPLE_CODE = "SAMPLE_CODE";
	public static final String SAMPLE_ACCESS_TOKEN = "SAMPLE_ACCESS_TOKEN";
	public static final String SAMPLE_REFRESH_TOKEN = "SAMPLE_REFRESH_TOKEN";

	public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	public static final String AUTHORIZATION_HEADER_VALUE = "Bearer " + SAMPLE_ACCESS_TOKEN;
	public static final String BEARER_TOKEN_PREFIX = "Bearer";
	public static final String FHIR_SERVER_PATH = "/r4";

	public static final String SAMPLE_PUBLIC_CLIENT_ID = "SAMPLE_PUBLIC_CLIENT_ID";
	public static final String SAMPLE_CONFIDENTIAL_CLIENT_ID = "SAMPLE_CONFIDENTIAL_CLIENT_ID";
	public static final String SAMPLE_CONFIDENTIAL_CLIENT_SECRET = "SAMPLE_CONFIDENTIAL_CLIENT_SECRET";
	
	public static final String DEFAULT_SCOPE = "launch/patient patient/*";



	public static String getServerBaseUrl(HttpServletRequest request) {
		String serverBaseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
		return serverBaseUrl;
	}

	public static String getFhirServerBaseUrl(HttpServletRequest request) {
		return getServerBaseUrl(request) + FHIR_SERVER_PATH;
	}
	
	public static String getSmartStyleUrl(HttpServletRequest request) {
		return getServerBaseUrl(request) + "/smart-style-url";
	}
	
	public static String createCode(String actualCode, String scopes, String patientId)
	{
		String encodedScope = Base64.getEncoder().encodeToString(scopes.getBytes());
		String encodedPatientId = Base64.getEncoder().encodeToString(patientId.getBytes());

		return actualCode + "." + encodedScope + "." + encodedPatientId;
	}
	
	public static String createAuthorizationHeaderValue(String accessToken, String scopes)
	{
		String encodedScopes = Base64.getEncoder().encodeToString(scopes.getBytes());
		return BEARER_TOKEN_PREFIX + " " + accessToken + "." + encodedScopes;
				
	}

}
