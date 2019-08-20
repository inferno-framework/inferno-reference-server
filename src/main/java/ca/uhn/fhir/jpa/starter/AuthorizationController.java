package ca.uhn.fhir.jpa.starter;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/oauth")
public class AuthorizationController {
	
	private static final String SAMPLE_TOKEN = "SAMPLE_TOKEN";
	
	@PostMapping("/token")
	public String getToken(@RequestBody String code, @RequestBody String scopes)
	{
		if (code.equals(SAMPLE_TOKEN))
		{
			String tokenString = 
					"{"
					+ "token : \"123\""
					+ "}";		
			return tokenString;
		}
		
		throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
		
		
	}
}
