package gov.onc.authorization;

import javax.annotation.PostConstruct;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.github.dnault.xmlpatch.internal.Log;

@RestController
@RequestMapping("/oauth")
public class AuthorizationController {
	
	private static final String SAMPLE_CODE = "SAMPLE_CODE";
	private static final String SAMPLE_ACCESS_TOKEN = "SAMPLE_ACCESS_TOKEN";
	private static final String SAMPLE_SCOPE = "launch launch/patient offline_access openid profile user/*.* patient/*.* fhirUser";
	private static final String SAMPLE_REFRESH_TOKEN = "SAMPLE_REFRESH_TOKEN";
	private static final String REFRESH_TOKEN_PARAM = "refresh_token";
	private static final String CODE_PARAM = "code";

	
	@PostConstruct
	protected void postConstruct(){
		Log.info("Authorization Controller added");
	}
		
	@PostMapping("/token")
	//public String getToken(@RequestBody Map<String,String> params)
	public ResponseEntity<String> getToken(@RequestParam(name="\"code\"",required=false) String code)
	//public ResponseEntity<String> getToken(@RequestBody MultiValueMap<String, String> params)
	{
		
		/*if (params.get(REFRESH_TOKEN_PARAM) != null)
		{
			return generateBearerTokenResponse();
		}
		
		Log.info("input params");
		for (String key : params.keySet())
		{
			Log.info("   key is " + key);
		}
				
		String code = params.getFirst(CODE_PARAM);*/

		if (code.equals(SAMPLE_CODE))
		{
			return generateBearerTokenResponse();
		}
		
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");

	}
	
	private ResponseEntity<String> generateBearerTokenResponse()
	{
		HttpHeaders headers = new HttpHeaders();
		headers.setCacheControl(CacheControl.noStore());
		headers.setPragma("no-cache");
		
		String tokenJSONString = generateBearerToken();
		ResponseEntity<String> responseEntity = new ResponseEntity<>(tokenJSONString, headers, HttpStatus.OK);

		return responseEntity;
	}
	
	/**
	 * Generates Token in Oauth2 expected format
	 * @return token JSON String
	 */
	private String generateBearerToken()
	{
		String tokenString = "{"
				+ "\"access_token\":\"" + SAMPLE_ACCESS_TOKEN + "\","
				+ "\"token_type\":\"bearer\","
				+ "\"expires_in\":3600,"
				+ "\"refresh_token\":\""+ SAMPLE_REFRESH_TOKEN +"\","
				+ "\"scope\":\""+SAMPLE_SCOPE+"\""
				+ "}";
		
		return tokenString;
	}
}
