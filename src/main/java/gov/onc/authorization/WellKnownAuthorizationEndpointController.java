package gov.onc.authorization;


import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.github.dnault.xmlpatch.internal.Log;

import ca.uhn.fhir.rest.annotation.Operation;

//@RestController
//@RequestMapping("/.well-known")
public class WellKnownAuthorizationEndpointController {
	
	private static final String WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";
	private static final String WELL_KNOWN_TOKEN_ENDPOINT_KEY = "token_endpoint";
	
	@PostConstruct
	protected void postConstruct() {
		Log.info("Well Known Authorization Controller added");
	}
	
	/**
	 * Get request to support well-known endpoints for authorization metadata. See
	 * http://www.hl7.org/fhir/smart-app-launch/conformance/index.html#using-well-known
	 * 
	 * @return String representing json object of metadata returned at this url
	 * @throws IOException 
	 */
	//@GetMapping("/smart-configuration")
	@Operation(name="/.well-known/smart-configuration", idempotent=true)
	public String getWellKnownJSON() {
		
		String wellKnownJSON = "{" 
				+ WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY + " : " + ServerConformanceWithAuthorizationProvider.AUTHORIZE_EXTENSION_VALUE_URI 
				+ ", "
				+ WELL_KNOWN_TOKEN_ENDPOINT_KEY + " : " + ServerConformanceWithAuthorizationProvider.TOKEN_EXTENSION_VALUE_URI + "}";
		
		ResponseEntity<String> wellKnownResponseEntity = new ResponseEntity<String>(wellKnownJSON, HttpStatus.ACCEPTED);
		return wellKnownJSON;
		//theServletResponse.setContentType("application/json");
		//theServletResponse.getOutputStream().print(wellKnownJSON);
		
		//bundle.addEntry(t)
	}
}
