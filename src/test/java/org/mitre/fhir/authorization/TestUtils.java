package org.mitre.fhir.authorization;

import java.util.Base64;
import java.util.Base64.Encoder;

import org.mitre.fhir.utils.FhirReferenceServerUtils;

public class TestUtils {

	public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	public static final String AUTHORIZATION_HEADER_BEARER_VALUE = "Bearer SAMPLE_ACCESS_TOKEN";

	public static String getBasicAuthorizationString(String clientId, String clientSecret) {
		return clientId + ":" + clientSecret;
	}

	public static String getEncodedBasicAuthorizationHeader() {
		return getEncodedBasicAuthorizationHeader(FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_ID,
				FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_SECRET);
	}

	public static String getEncodedBasicAuthorizationHeader(String clientId, String clientSecret) {
		Encoder encoder = Base64.getUrlEncoder();
		String decodedValue = getBasicAuthorizationString(clientId, clientSecret);
		String encodedValue = encoder.encodeToString(decodedValue.getBytes());
		return "Basic " + encodedValue;
	}

}
