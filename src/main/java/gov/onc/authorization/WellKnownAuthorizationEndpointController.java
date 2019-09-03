package gov.onc.authorization;


import javax.annotation.PostConstruct;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.github.dnault.xmlpatch.internal.Log;

@RestController
@RequestMapping("/.well-known")
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
	 */
	@GetMapping("/smart-configuration")
	public String getWellKnownJSON() {
		String wellKnownJSON = "{" + WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY + " : "
				+ ServerConformanceWithAuthorizationProvider.AUTHORIZE_EXTENSION_URL + WELL_KNOWN_TOKEN_ENDPOINT_KEY
				+ ", " 
				+ " : " + ServerConformanceWithAuthorizationProvider.TOKEN_EXTENSION_URL + "}";
		return wellKnownJSON;
	}
}
