package org.mitre.fhir.authorization;

import javax.annotation.PostConstruct;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.github.dnault.xmlpatch.internal.Log;

@RestController
public class AuthorizationController {

	private static final String SAMPLE_CODE = "SAMPLE_CODE";
	private static final String SAMPLE_ACCESS_TOKEN = "SAMPLE_ACCESS_TOKEN";
	private static final String SAMPLE_SCOPE = "launch launch/patient offline_access openid profile user/*.* patient/*.* fhirUser";
	private static final String SAMPLE_REFRESH_TOKEN = "SAMPLE_REFRESH_TOKEN";

	@PostConstruct
	protected void postConstruct() {
		Log.info("Authorization Controller added");
	}

	/**
	 * Provide a code to get a bearer token for authorization 
	 * 
	 * @param code
	 * @return bearer token to be used for authorization
	 */
	@PostMapping("/token")
	public ResponseEntity<String> getToken(@RequestParam(name = "code", required = false) String code) {

		Log.info("code is " + code);

		if (SAMPLE_CODE.equals(code)) {
			return generateBearerTokenResponse();
		}

		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
	}
	
	private ResponseEntity<String> generateBearerTokenResponse() {
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(CacheControl.noStore());
		headers.setPragma("no-cache");

		String tokenJSONString = generateBearerToken();
		ResponseEntity<String> responseEntity = new ResponseEntity<String>(tokenJSONString, headers, HttpStatus.OK);

		return responseEntity;
	}

	/**
	 * Generates Token in Oauth2 expected format
	 * 
	 * @return token JSON String
	 */
	private String generateBearerToken() {
		String tokenString = 
				"{" 
						+ "\"access_token\":\"" + SAMPLE_ACCESS_TOKEN + "\","
						+ "\"token_type\":\"bearer\","
						+ "\"expires_in\":3600," 
						+ "\"refresh_token\":\"" + SAMPLE_REFRESH_TOKEN + "\"," 
						+ "\"scope\":\"" + SAMPLE_SCOPE + "\"," 
						+ "\"id_token\":" + generateSampleOpenIdToken() 
				+ "}";

		return tokenString;
	}
	/**
	 * Generates a sample open id token https://openid.net/specs/openid-connect-core-1_0.html
	 * 
	 * @return token JSON String representing the open id token
	 */
	private String generateSampleOpenIdToken()
	{
		String sampleOpenIdTokenString =
				"{"
					+ "\"iss\": \"https://server.example.com\","
				    + "\"sub\": 24400320,"
				    + "\"aud\": \"s6BhdRkqt3\","
				    + "\"nonce\": \"n-0S6_WzA2Mj\","
				    + "\"exp\": 1311281970,"
				    + "\"iat\": 1311280970,"
				    + "\"auth_time\": 1311280969,"
				    + "\"acr\": \"urn:mace:incommon:iap:silver\""
				 + "}";
		return sampleOpenIdTokenString;
	}
}
