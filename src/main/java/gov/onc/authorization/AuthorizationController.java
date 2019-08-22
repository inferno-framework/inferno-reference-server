package gov.onc.authorization;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.github.dnault.xmlpatch.internal.Log;

@RestController
@RequestMapping("/oauth")
public class AuthorizationController {
	
	private static final String SAMPLE_CODE = "SAMPLE_CODE";
	
	@PostConstruct
	protected void postConstruct(){
		Log.info("Authorization Controller added");
	}
		
	@PostMapping("/token")
	public String getToken(@RequestBody Map<String,String> params)
	{
		String code = params.get("code");
		String scopes = params.get("scopes");
		Log.info("code is " + code);
		Log.info("scopes is " + scopes);

		if (code.equals(SAMPLE_CODE))
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
