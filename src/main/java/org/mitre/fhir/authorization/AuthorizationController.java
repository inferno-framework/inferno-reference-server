
package org.mitre.fhir.authorization;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.dnault.xmlpatch.internal.Log;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.json.JSONObject;
import org.mitre.fhir.HapiReferenceServerProperties;
import org.mitre.fhir.authorization.exception.BearerTokenException;
import org.mitre.fhir.authorization.exception.InvalidBearerTokenException;
import org.mitre.fhir.authorization.exception.InvalidClientIdException;
import org.mitre.fhir.authorization.exception.InvalidClientSecretException;
import org.mitre.fhir.authorization.exception.InvalidScopesException;
import org.mitre.fhir.authorization.exception.OpenIdTokenGenerationException;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.authorization.token.TokenNotFoundException;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.FhirUtils;
import org.mitre.fhir.utils.RsaUtils;
import org.mitre.fhir.utils.exception.RsaKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@RestController
public class AuthorizationController {

  private static final String BULK_EXPECTED_GRANT_TYPE = "client_credentials";
  private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
  private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
  private static final String BULK_EXPECTED_CLIENT_ASSERTION_TYPE =
      "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

  @PostConstruct
  protected void postConstruct() {
    Log.info("Authorization Controller added");
  }

  /**
   * Get service to validate the client id.
   * 
   * @param clientId client id to be validated
   * @param request the web service request
   * @return string representation of the bundle of all patients on the endpoint given by the
   *         request's base url
   */
  @GetMapping(path = "authorizeClientId/{clientId}", produces = {"application/json"})
  public String validateClientId(@PathVariable String clientId, HttpServletRequest request) {
    authorizeClientId(clientId);
    IGenericClient client = FhirReferenceServerUtils.getClientFromRequest(request);
    Bundle patientsBundle = FhirUtils.getPatientsBundle(client);
    return FhirContext.forR4().newJsonParser().encodeResourceToString(patientsBundle);
  }

  /**
   * Get service to validate the client id.
   * 
   * @param scopeString client id to be validated
   * @param grantType the web service request
   * @param clientAssertionType the client assertion_type
   * @param clientAssertion the client assertion
   * @return auth token
   */
  public ResponseEntity<String> getTokenByBackendServiceAuthorization(
      String scopeString,
      String grantType,
      String clientAssertionType,
      String clientAssertion,
      HttpServletRequest request) throws BearerTokenException {

    // validate scopes
    validateBulkDataScopes(scopeString);

    // check grant_type
    if (!BULK_EXPECTED_GRANT_TYPE.equals(grantType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Grant Type should be " + BULK_EXPECTED_GRANT_TYPE);
    }

    // check client_assertion_type
    if (!BULK_EXPECTED_CLIENT_ASSERTION_TYPE.equals(clientAssertionType)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Client Assertion Type should be " + BULK_EXPECTED_CLIENT_ASSERTION_TYPE);
    }

    // validate client_assertion (jwt)
    DecodedJWT decodedJwt = JWT.decode(clientAssertion);

    String clientId = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InJlZ2lzdHJhdGlvbi10b2tlbiJ9"
        + ".eyJqd2tzX3VybCI6Imh0dHA6Ly8xMC4xNS4yNTIuNzMvaW5mZXJuby8ud2VsbC1rbm93bi"
        + "9qd2tzLmpzb24iLCJhY2Nlc3NUb2tlbnNFeHBpcmVJbiI6MTUsImlhdCI6MTU5NzQxMzE5NX"
        + "0.q4v4Msc74kN506KTZ0q_minyapJw0gwlT6M_uiL73S4";
    if (!clientId.equals(decodedJwt.getIssuer())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Issuer should be " + BULK_EXPECTED_CLIENT_ASSERTION_TYPE);
    }

    TokenManager tokenManager = TokenManager.getInstance();
    Token token = tokenManager.createToken(scopeString);
    int expiresIn = 300;

    JSONObject accessToken = new JSONObject();
    accessToken.put("token_type", "bearer");
    accessToken.put("scope", scopeString);
    accessToken.put("expires_in", expiresIn);
    accessToken.put("access_token", token.getTokenValue());

    HttpHeaders headers = new HttpHeaders();

    return new ResponseEntity<>(accessToken.toString(), headers, HttpStatus.OK);
  }

  private void validateBulkDataScopes(String scopesString) {
    List<String> scopes = FhirReferenceServerUtils.getScopesListByScopeString(scopesString);

    List<String> invalidScopes = new ArrayList<>();

    for (String scope : scopes) {
      // confirm scope is system level and a valid scope
      if (!scope.startsWith("system/")) {
        invalidScopes.add(scope);
      } else {
        String[] scopeParts = scope.split("\\.");
        String scopeAction = scopeParts[1];

        List<String> actionPatterns = List.of("read", "write", "\\*", "c?ru?d?s?");

        Boolean validAction = false;

        for (String pattern : actionPatterns) {
          Pattern r = Pattern.compile(pattern);
          Matcher m = r.matcher(scopeAction);
          if (m.find()) {
            validAction = true;
          }
        }

        if (!validAction) {
          invalidScopes.add(scope);
        }
      }
    }

    if (!invalidScopes.isEmpty()) {
      StringBuilder invalidScopesString = new StringBuilder();
      for (String invalidScope : invalidScopes) {
        invalidScopesString.append(invalidScope).append(", ");
      }

      // strip of last 2 characters
      invalidScopesString = new StringBuilder(invalidScopesString.substring(0,
       invalidScopesString.length() - 1));

      String message = "The following scopes are invalid for bulk data : " + invalidScopesString;
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message,
          new InvalidScopesException(message));
    }
  }

  /**
   * Provide a code to get a bearer token for authorization.
   *
   * @param code code to get token
   * @return bearer token to be used for authorization
   * @throws BearerTokenException error generating a bearer token
   */
  @PostMapping(path = "/token", produces = {"application/json"})
  public ResponseEntity<String> getToken(
      @RequestParam(name = "code", required = false) String code,
      @RequestParam(name = "client_id", required = false) String clientIdRequestParam,
      @RequestParam(name = "refresh_token", required = false) String refreshTokenValue,
      @RequestParam(name = "code_verifier", required = false) String codeVerifier,
      @RequestParam(name = "grant_type", required = true) String grantType,
      @RequestParam(name = "scope", required = false) String scopes,
      @RequestParam(name = "client_assertion_type", required = false) String clientAssertionType,
      @RequestParam(name = "client_assertion", required = false) String clientAssertion,
      HttpServletRequest request
  ) throws ResponseStatusException, BearerTokenException {

    Log.info("code is " + code);

    if (BULK_EXPECTED_GRANT_TYPE.equals(grantType)) {
      return getTokenByBackendServiceAuthorization(
                                                   scopes,
                                                   grantType,
                                                   clientAssertionType,
                                                   clientAssertion,
                                                   request
                                                  );
    } else if (!(AUTHORIZATION_CODE_GRANT_TYPE.equals(grantType)
              || REFRESH_TOKEN_GRANT_TYPE.equals(grantType))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Bad Grant Type: " + grantType);
    }

    String clientId = validateClient(request, clientIdRequestParam);

    String patientId = "";
    String encounterId = "";

    if (code != null) {
      return validateCode(request, code, clientId, codeVerifier);
    } else if (refreshTokenValue != null) {

      try {
        if (TokenManager.getInstance().authenticateRefreshToken(refreshTokenValue)) {
          Token refreshToken = TokenManager.getInstance().getRefreshToken(refreshTokenValue);
          patientId = refreshToken.getPatientId();
          encounterId = refreshToken.getEncounterId();
          String refreshTokenScopes = refreshToken.getScopesString();
          return generateBearerTokenResponse(request, clientId, refreshTokenScopes, patientId,
           encounterId);
        }
      } catch (TokenNotFoundException | InvalidBearerTokenException tokenNotFoundException) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Refresh Token " + refreshTokenValue + " was not found");
      }
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                      "No code or refresh token provided.");
  }

  private static String validateClient(HttpServletRequest request, String clientIdRequestParam) {
    // check client id and client secret if the server is confidential
    String basicHeader = getBasicHeader(request);

    String clientId;
    String clientSecret = null;

    // if basic header exists, extract clientId and clientSecret from basic header
    if (basicHeader != null) {
      String decodedValue = getDecodedBasicAuthorizationString(basicHeader);
      String[] splitDecodedValue = decodedValue.split(":");
      // client id is username, and should be before ':'
      clientId = splitDecodedValue[0];
      // client secret is password, and should be after ':'

      clientSecret = splitDecodedValue.length >= 2 ? splitDecodedValue[1] : "";
    } else {
      // if no basic auth, client id should be supplied as request param
      clientId = clientIdRequestParam;
    }

    authenticateClientIdAndClientSecret(clientId, clientSecret);

    return clientId;
  }

  private ResponseEntity<String> validateCode(
      HttpServletRequest request,
      String encodedCodeString,
      String clientId,
      String codeVerifier
  ) throws ResponseStatusException, BearerTokenException {
    try {
      String rawCodeString = new String(Base64.getDecoder().decode(encodedCodeString));
      JSONObject codeObject = new JSONObject(rawCodeString);
      String scopes = null;
      String patientId = null;
      String encounterId = null;
      String codeChallenge = null;
      String codeChallengeMethod = null;
      String code = null;

      if (codeObject.has("scopes")) {
        scopes = (String) codeObject.get("scopes");
      }
      if (codeObject.has("patientId")) {
        patientId = (String) codeObject.get("patientId");
      }
      if (codeObject.has("encounterId")) {
        encounterId = (String) codeObject.get("encounterId");
      }
      if (codeObject.has("codeChallenge")) {
        codeChallenge = (String) codeObject.get("codeChallenge");
      }
      if (codeObject.has("codeChallengeMethod")) {
        codeChallengeMethod = (String) codeObject.get("codeChallengeMethod");
      }
      if (codeObject.has("code")) {
        code = (String) codeObject.get("code");
      }

      validatePkce(codeChallengeMethod, codeChallenge, codeVerifier, scopes);

      if (code != null && FhirReferenceServerUtils.SAMPLE_CODE.equals(code)) {
        return generateBearerTokenResponse(request, clientId, scopes, patientId, encounterId);
      }
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
    }

    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
  }

  private void validatePkce(
        String codeChallengeMethod,
        String codeChallenge,
        String codeVerifier,
        String scopes
  ) throws ResponseStatusException {
    String[] scopeList = scopes == null ? new String[0] : scopes.split(" ");

    Boolean v2ScopeFound = false;
    String v2ScopePattern = "\\b(patient|user|system|\\*)/[\\w*]\\.c?r?u?d?s?\\b";
    for (String scope : scopeList) {
      if (scope.matches(v2ScopePattern)) {
        v2ScopeFound = true;
        break;
      }
    }

    if (codeChallenge == null && codeVerifier == null && !v2ScopeFound) {
      return;
    }

    if (codeChallengeMethod != null && !"S256".equalsIgnoreCase(codeChallengeMethod)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST,
          "Only S256 PKCE code challenge method is supported"
      );
    }

    if (codeChallenge == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No code challenge received");
    }

    if (codeVerifier == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No code verifier received");
    }

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] rawHash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
      String hash = Base64.getUrlEncoder().withoutPadding().encodeToString(rawHash);

      if (!codeChallenge.equalsIgnoreCase(hash)) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code verifier");
      }
    } catch (NoSuchAlgorithmException exception) {
      // This should not be reachable
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unable to process code verifier");
    }

    return;
  }

  private ResponseEntity<String> generateBearerTokenResponse(HttpServletRequest request,
      String clientId, String scopes, String patientId, String encounterId)
        throws BearerTokenException {
    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl(CacheControl.noStore());
    headers.setPragma("no-cache");

    String tokenJsonString = generateBearerToken(request, clientId, scopes, patientId, encounterId);
    return new ResponseEntity<>(tokenJsonString, headers, HttpStatus.OK);
  }

  /**
   * Generates Token in Oauth2 expected format.
   *
   * @return token JSON String
   * @throws BearerTokenException if token generation runs into an error
   */
  private String generateBearerToken(HttpServletRequest request, String clientId, String scopes,
      String patientId, String encounterId) throws BearerTokenException {

    IGenericClient client = FhirReferenceServerUtils.getClientFromRequest(request);
    JSONObject tokenJson = new JSONObject();

    Long expiresIn = 3600L;
    TokenManager tokenManager = TokenManager.getInstance();
    Token token = tokenManager.createToken(scopes);
    token.setClientId(clientId);
    token.setPatientId(patientId);
    token.setEncounterId(encounterId);
    token.setExp(java.time.Instant.now().getEpochSecond() + expiresIn );

    String refreshTokenValue;
    try {
      Token refreshToken = tokenManager.getCorrespondingRefreshToken(token.getTokenValue());
      refreshToken.setPatientId(patientId);
      refreshToken.setEncounterId(encounterId);
      refreshToken.setClientId(clientId);
      refreshTokenValue = refreshToken.getTokenValue();
    } catch (Exception exception) {
      throw new BearerTokenException(exception);
    }

    String accessToken = token.getTokenValue();

    tokenJson.put("access_token", accessToken);
    tokenJson.put("token_type", "bearer");
    tokenJson.put("expires_in", expiresIn);
    tokenJson.put("refresh_token", refreshTokenValue);
    tokenJson.put("scope", scopes);
    tokenJson.put("smart_style_url", FhirReferenceServerUtils.getSmartStyleUrl(request));
    tokenJson.put("need_patient_banner", false);

    if ("".equals(patientId) || patientId == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No patients found");
    }

    List<String> scopesList = FhirReferenceServerUtils.getScopesListByScopeString(scopes);

    if (scopesList.contains("launch") || scopesList.contains("launch/patient")) {
      tokenJson.put("patient", patientId);
    }

    if (scopesList.contains("launch") || scopesList.contains("launch/encounter")) {
      if (Objects.equals(encounterId, "") || encounterId == null) {
        Encounter encounter = getFirstEncounterByPatientId(client, patientId);

        if (encounter == null) {
          throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No encounters found");
        }

        encounterId = encounter.getIdElement().getIdPart();
      }

      tokenJson.put("encounter", encounterId);
    }

    if(scopesList.contains("openid") && (scopesList.contains("fhirUser") || scopesList.contains("profile"))){
      try {
        tokenJson.put("id_token", generateSampleOpenIdToken(request, clientId, patientId));
      } catch (OpenIdTokenGenerationException openIdTokenGenerationException) {
        throw new BearerTokenException(openIdTokenGenerationException);
      }
    }
    return tokenJson.toString();
  }

  /**
   * Generates a sample open id token https://openid.net/specs/openid-connect-core-1_0.html
   *
   * @return token JSON String representing the open id token
   * @throws OpenIdTokenGenerationException if error generating open id token
   */
  private String generateSampleOpenIdToken(HttpServletRequest request, String clientId,
      String patientId) throws OpenIdTokenGenerationException {

    try {
      RSAPublicKey publicKey = RsaUtils.getRsaPublicKey();
      RSAPrivateKey privateKey = RsaUtils.getRsaPrivateKey();

      // for now hard coding as a Patient
      // http://hl7.org/fhir/smart-app-launch/worked_example_id_token/index.html#Encode-them-in-a-JWT
      String fhirUserUrl =
          FhirReferenceServerUtils.getFhirServerBaseUrl(request) + "/Patient/" + patientId;

      Calendar calendar = Calendar.getInstance();

      Date issuedAt = calendar.getTime();
      calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
      Date expiresAt = calendar.getTime();

      Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
      return JWT.create().withIssuer(FhirReferenceServerUtils.getFhirServerBaseUrl(request))
          .withSubject(TokenManager.SUB_STRING).withAudience(clientId).withExpiresAt(expiresAt).withIssuedAt(issuedAt)
          .withClaim("fhirUser", fhirUserUrl).sign(algorithm);
    } catch (RsaKeyException rsaKeyException) {
      throw new OpenIdTokenGenerationException(rsaKeyException);
    }
  }

  private Encounter getFirstEncounterByPatientId(IGenericClient client, String patientId) {
    Encounter encounter = null;

    Token token = TokenManager.getInstance().getServerToken();

    Bundle encountersBundle =
        client.search().forResource(Encounter.class).where(Encounter.PATIENT.hasId(patientId))
            .returnBundle(Bundle.class).cacheControl(new CacheControlDirective().setNoCache(true))
            .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
                FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
            .execute();
    List<BundleEntryComponent> encounters = encountersBundle.getEntry();

    for (BundleEntryComponent bundleEntryComponent : encounters) {
      if ("Encounter".equals(bundleEntryComponent.getResource().fhirType())) {
        encounter = (Encounter) bundleEntryComponent.getResource();
        break;
      }
    }

    return encounter;
  }

  private static String getBasicHeader(HttpServletRequest request) {
    Enumeration<String> authorizationHeaders = request.getHeaders("Authorization");
    // find Basic Auth
    String basicHeader = null;
    while (authorizationHeaders.hasMoreElements()) {
      String header = authorizationHeaders.nextElement();
      if (header.startsWith("Basic ")) {
        basicHeader = header;
        break;
      }
    }

    return basicHeader;
  }

  private static String getDecodedBasicAuthorizationString(String basicHeader) {
    String encodedValue = basicHeader.replaceFirst("Basic ", ""); // strip off the beginning
    Decoder decoder = Base64.getUrlDecoder();
    return new String(decoder.decode(encodedValue));
  }

  private static void authorizeClientId(String clientId) {
    HapiReferenceServerProperties properties = new HapiReferenceServerProperties();
    if (!properties.getPublicClientId().equals(clientId)
        && !properties.getConfidentialClientId().equals(clientId)) {
      throw new InvalidClientIdException(clientId);
    }
  }

  private static void authenticateClientIdAndClientSecret(String clientId, String clientSecret) {

    HapiReferenceServerProperties properties = new HapiReferenceServerProperties();

    authorizeClientId(clientId);

    if (properties.getConfidentialClientId().equals(clientId)
        && !properties.getConfidentialClientSecret().equals(clientSecret)) {
      throw new InvalidClientSecretException();
    }
  }

}
