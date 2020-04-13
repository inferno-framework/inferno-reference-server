package org.mitre.fhir.utils;

import java.util.Base64;


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
	
	private static final String DEFAULT_SERVER_BASE_URL = "http://localhost:1234";
	
	public static final String DEFAULT_SCOPE = "launch/patient patient/*";

	public static String getServerBaseUrl() {
		String serverBaseURL = System.getenv("SERVER_BASE_URL");
		if (serverBaseURL == null || serverBaseURL.equals(""))
		{
			//use default value
			return DEFAULT_SERVER_BASE_URL;
		}
		
		return serverBaseURL;
	}

	public static String getFhirServerBaseUrl() {
		return getServerBaseUrl() + FHIR_SERVER_PATH;
	}
	
	public static String getSmartStyleUrl() {
		return getServerBaseUrl() + "/smart-style-url";
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
