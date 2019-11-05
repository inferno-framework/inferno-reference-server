package org.mitre.fhir.utils;

import javax.servlet.http.HttpServletRequest;

public class FhirReferenceServerUtils {
	
	public static final String SAMPLE_CODE = "SAMPLE_CODE";
	public static final String SAMPLE_ACCESS_TOKEN = "SAMPLE_ACCESS_TOKEN";
	public static final String SAMPLE_REFRESH_TOKEN = "SAMPLE_REFRESH_TOKEN";

	public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	public static final String AUTHORIZATION_HEADER_VALUE = "Bearer " + SAMPLE_ACCESS_TOKEN;
	public static final String FHIR_SERVER_PATH = "/r4";

	public static final String SAMPLE_CLIENT_ID = "SAMPLE_CLIENT_ID";
	
	public static String getServerBaseUrl(HttpServletRequest request)
	{
		String serverBaseUrl = request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();
		return serverBaseUrl;
	}
	
	public static String getFhirServerBaseUrl(HttpServletRequest request)
	{
		return getServerBaseUrl(request) + FHIR_SERVER_PATH;
	}
}
