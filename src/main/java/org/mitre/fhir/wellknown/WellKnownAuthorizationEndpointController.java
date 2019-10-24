package org.mitre.fhir.wellknown;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.github.dnault.xmlpatch.internal.Log;
import org.mitre.fhir.authorization.ServerConformanceWithAuthorizationProvider;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.RSAUtils;

@RestController
public class WellKnownAuthorizationEndpointController {

	private static final String WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY = "\"authorization_endpoint\"";
	private static final String WELL_KNOWN_TOKEN_ENDPOINT_KEY = "\"token_endpoint\"";
	private static final String WELL_KNOWN_CAPABILITIES_KEY = "\"capabilities\"";
	private static final String WELL_KNOWN_JWK_URI_KEY = "\"jwks_uri\"";

	// see 2.1
	// http://hl7.org/fhir/smart-app-launch/conformance/index.html#core-capabilities
	private static final String WELL_KNOWN_CAPABILITIES_VALUES = "[" + "\"launch-ehr\"," + "\"launch-standalone\","
			+ "\"client-public\"," + "\"client-confidential-symmetric\"," + "\"sso-openid-connect\","
			+ "\"context-banner\"," + "\"context-style\"," + "\"context-ehr-patient\"," + "\"context-ehr-encounter\","
			+ "\"context-standalone-patient\"," + "\"context-standalone-encounter\"," + "\"permission-offline\","
			+ "\"permission-patient\"," + "\"permission-user\"" + "]";

	@PostConstruct
	protected void postConstruct() {
		Log.info("Well Known Authorization Controller added.");
	}

	/**
	 * Get request to support well-known endpoints for authorization metadata. See
	 * http://www.hl7.org/fhir/smart-app-launch/conformance/index.html#using-well-known
	 * 
	 * @return String representing json object of metadata returned at this url
	 * @throws IOException
	 */
	@GetMapping(path = "/smart-configuration", produces = { "application/json" })
	public String getWellKnownJSON(HttpServletRequest theRequest) {

		String wellKnownJSON = "{" + WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY + " : \""
				+ ServerConformanceWithAuthorizationProvider.getAuthorizationExtensionURI(theRequest) + "\"" + ", "
				+ WELL_KNOWN_TOKEN_ENDPOINT_KEY + " : \""
				+ ServerConformanceWithAuthorizationProvider.getTokenExtensionURI(theRequest) + "\"" + ", "
				+ WELL_KNOWN_CAPABILITIES_KEY + " : " + WELL_KNOWN_CAPABILITIES_VALUES + "}";

		return wellKnownJSON;
	}

	@GetMapping(path = "/openid-configuration", produces = { "application/json" })
	public String getSmartConfiguration(HttpServletRequest theRequest) {

		String uri = "\"" + FhirReferenceServerUtils.getFhirServerBaseUrl(theRequest) + "/.well-known/jwk\"";

		String smartConfig = "{" + WELL_KNOWN_JWK_URI_KEY + ":" + uri + "}";

		return smartConfig;
	}

	@GetMapping(path = "/jwk", produces = { "application/json" })
	public String getJWK(HttpServletRequest theRequest) {

		Base64.Encoder encoder = Base64.getUrlEncoder();

		RSAPublicKey publicKey = RSAUtils.getRSAPublicKey();
		byte[] modulus = publicKey.getModulus().toByteArray();
		byte[] exponent = publicKey.getPublicExponent().toByteArray();

		String algorithm = "\"RS256\"";
		String kty = "\"RSA\"";
		String use = "\"sig\"";

		String encodedModulus = "\"" + encoder.encodeToString(modulus) + "\"";
		String encodedExponent = "\"" + encoder.encodeToString(exponent) + "\"";

		String jwk = "{ \"keys\": [" + "{" + "\"alg\": " + algorithm + "," + "\"kty\": " + kty + "," + "\"use\": " + use
				+ "," + "\"n\": " + encodedModulus + "," + "\"e\": " + encodedExponent + "}" + "]}";

		return jwk;

	}
}
