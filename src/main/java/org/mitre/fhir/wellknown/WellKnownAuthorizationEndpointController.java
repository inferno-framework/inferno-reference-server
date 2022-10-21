package org.mitre.fhir.wellknown;

import com.github.dnault.xmlpatch.internal.Log;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.apache.jena.atlas.json.JSON;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mitre.fhir.authorization.ServerConformanceWithAuthorizationProvider;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.RsaUtils;
import org.mitre.fhir.utils.exception.RsaKeyException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WellKnownAuthorizationEndpointController {

  private static final String WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";
  private static final String WELL_KNOWN_TOKEN_ENDPOINT_KEY = "token_endpoint";
  private static final String WELL_KNOWN_REVOCATION_ENDPOINT_KEY = "revocation_endpoint";
  private static final String WELL_KNOWN_CAPABILITIES_KEY = "capabilities";
  private static final String WELL_KNOWN_JWK_URI_KEY = "jwks_uri";
  private static final String WELL_KNOWN_GRANT_TYPES_SUPPORTED_KEY = "grant_types_supported";
  private static final String WELL_KNOWN_CODE_CHALLENGE_METHODS_SUPPORTED_KEY = "code_challenge_methods_supported";

  // 2.1 on
  // http://hl7.org/fhir/smart-app-launch/conformance/index.html#core-capabilities
  private static final String[] capabilityValues = {
      "launch-ehr",
      "launch-standalone",
      "client-public",
      "client-confidential-symmetric",
      "sso-openid-connect",
      "context-banner",
      "context-style",
      "context-ehr-patient",
      "context-ehr-encounter",
      "context-standalone-patient",
      "context-standalone-encounter",
      "permission-offline",
      "permission-patient",
      "permission-user"
      };

  private static final String[] grantTypesSupportedValues = {
      "authorization_code",
      "client_credentials"
  };
  private static final String[] codeChallengeMethodsSupportedValues = { "S256" };

  private static final JSONArray WELL_KNOWN_CAPABILITIES_VALUES = new JSONArray(capabilityValues);
  private static final JSONArray WELL_KNOWN_GRANT_TYPES_SUPPORTED_VALUES =
        new JSONArray(grantTypesSupportedValues);
  private static final JSONArray WELL_KNOWN_CODE_CHALLENGE_METHODS_SUPPORTED_VALUES =
        new JSONArray(codeChallengeMethodsSupportedValues);

  @PostConstruct
  protected void postConstruct() {
    Log.info("Well Known Authorization Controller added.");
  }

  /**
   * Get request to support well-known endpoints for authorization metadata. See
   * http://www.hl7.org/fhir/smart-app-launch/conformance/index.html#using-well-known
   *
   * @return String representing json object of metadata returned at this url
   * @throws IOException when the request fails
   */
  @GetMapping(path = "/smart-configuration", produces = {"application/json"})
  public String getWellKnownJson(HttpServletRequest theRequest) {

    JSONObject wellKnownJson = new JSONObject();
    wellKnownJson.put(WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY,
        ServerConformanceWithAuthorizationProvider.getAuthorizationExtensionUri(theRequest));
    wellKnownJson.put(WELL_KNOWN_TOKEN_ENDPOINT_KEY,
        ServerConformanceWithAuthorizationProvider.getTokenExtensionUri(theRequest));
    wellKnownJson.put(WELL_KNOWN_REVOCATION_ENDPOINT_KEY,
        ServerConformanceWithAuthorizationProvider.getRevokeExtensionUri(theRequest));
    wellKnownJson.put(WELL_KNOWN_CAPABILITIES_KEY, WELL_KNOWN_CAPABILITIES_VALUES);
    wellKnownJson.put(WELL_KNOWN_GRANT_TYPES_SUPPORTED_KEY,
        WELL_KNOWN_GRANT_TYPES_SUPPORTED_VALUES);
    wellKnownJson.put(WELL_KNOWN_CODE_CHALLENGE_METHODS_SUPPORTED_KEY,
        WELL_KNOWN_CODE_CHALLENGE_METHODS_SUPPORTED_VALUES);

    return wellKnownJson.toString();
  }

  /**
   * Supports retrieval of the openid configuration for authorization and authentication.
   *
   * @param theRequest the incoming HTTP request
   * @return String representing the JSON object of the OpenID configuration
   */
  @GetMapping(path = "/openid-configuration", produces = {"application/json"})
  public String getOpenIdConfiguration(HttpServletRequest theRequest) {

    String uri = FhirReferenceServerUtils.getFhirServerBaseUrl(theRequest) + "/.well-known/jwk";

    JSONObject openIdConfigJson = new JSONObject();

    openIdConfigJson.put("issuer", FhirReferenceServerUtils.getFhirServerBaseUrl(theRequest));
    openIdConfigJson.put("authorization_endpoint",
        ServerConformanceWithAuthorizationProvider.getAuthorizationExtensionUri(theRequest));
    openIdConfigJson.put("token_endpoint",
        ServerConformanceWithAuthorizationProvider.getTokenExtensionUri(theRequest));
    openIdConfigJson.put(WELL_KNOWN_JWK_URI_KEY, uri);

    String[] responseTypesSupported = {"code", "id_token", "token id_token"};
    openIdConfigJson.put("response_types_supported", new JSONArray(responseTypesSupported));

    String[] subjectTypesSupported = {"pairwise", "public"};
    openIdConfigJson.put("subject_types_supported", new JSONArray(subjectTypesSupported));

    String[] idTokenSigningAlgorithmValuesSupported = {"RS256"};
    openIdConfigJson.put("id_token_signing_alg_values_supported",
        new JSONArray(idTokenSigningAlgorithmValuesSupported));

    return openIdConfigJson.toString();

  }

  /**
   * Returns the JWK for bulk data tests.
   * @param theRequest the incoming HTTP request
   * @return String representing the JWK
   * @throws RsaKeyException When a key creation failure happens
   */
  @GetMapping(path = "/jwk", produces = {"application/json"})
  public String getJwk(HttpServletRequest theRequest) throws RsaKeyException {

    Base64.Encoder encoder = Base64.getUrlEncoder();

    RSAPublicKey publicKey = RsaUtils.getRsaPublicKey();
    byte[] modulus = publicKey.getModulus().toByteArray();
    byte[] exponent = publicKey.getPublicExponent().toByteArray();

    String algorithm = "RS256";
    String kty = "RSA";
    String use = "sig";

    String encodedModulus = encoder.encodeToString(modulus);
    String encodedExponent = encoder.encodeToString(exponent);

    JSONObject firstKeyEntry = new JSONObject();
    firstKeyEntry.put("alg", algorithm);
    firstKeyEntry.put("kty", kty);
    firstKeyEntry.put("use", use);
    firstKeyEntry.put("n", encodedModulus);
    firstKeyEntry.put("e", encodedExponent);

    JSONArray keys = new JSONArray();
    keys.put(firstKeyEntry);

    JSONObject jwk = new JSONObject();
    jwk.put("keys", keys);

    return jwk.toString();

  }
}
