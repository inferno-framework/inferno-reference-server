package org.mitre.fhir.authorization;

import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.auth0.jwk.InvalidPublicKeyException;
import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.SigningKeyNotFoundException;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.dnault.xmlpatch.internal.Log;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.json.JSONObject;
import org.mitre.fhir.HapiReferenceServerProperties;
import org.mitre.fhir.authorization.exception.InvalidBearerTokenException;
import org.mitre.fhir.authorization.exception.InvalidClientIdException;
import org.mitre.fhir.authorization.exception.InvalidClientSecretException;
import org.mitre.fhir.authorization.exception.OAuth2Exception;
import org.mitre.fhir.authorization.exception.OAuth2Exception.ErrorCode;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

@RestController
public class AuthorizationController {

  private static final String CLIENT_CREDENTIALS_GRANT_TYPE = "client_credentials";
  private static final String AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code";
  private static final String REFRESH_TOKEN_GRANT_TYPE = "refresh_token";
  private static final String JWT_BEARER_CLIENT_ASSERTION_TYPE =
      "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";

  /**
   * A map of "known" granular search parameters by resource type.
   * Scopes with these parameters must be offered to users on the authorization screen
   * if they request resource-level scopes.
   */
  @SuppressWarnings("unchecked")
  private static final Map<String, List<String>> KNOWN_PARAMS =
      (Map<String, List<String>>) new Yaml().load(
          AuthorizationController.class.getResourceAsStream("/scope_parameters.yml"));

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
    authorizeClientId(clientId, false);
    IGenericClient client = FhirReferenceServerUtils.getClientFromRequest(request);
    Bundle patientsBundle = FhirUtils.getPatientsBundle(client);
    return FhirReferenceServerUtils.FHIR_CONTEXT_R4.newJsonParser()
        .encodeResourceToString(patientsBundle);
  }

  /**
   * Get service to validate the client id.
   * 
   * @param scopeString client id to be validated
   * @param clientAssertionType the client assertion_type
   * @param clientAssertion the client assertion
   * @return auth token
   */
  public ResponseEntity<String> getTokenByBackendServiceAuthorization(
      String scopeString,
      String clientAssertionType,
      String clientAssertion,
      HttpServletRequest request) {

    // validate scopes
    validateBulkDataScopes(scopeString);

    // check client_assertion_type
    if (!JWT_BEARER_CLIENT_ASSERTION_TYPE.equals(clientAssertionType)) {
      throw new OAuth2Exception(ErrorCode.INVALID_REQUEST,
          "Client Assertion Type should be " + JWT_BEARER_CLIENT_ASSERTION_TYPE);
    }

    // validate client_assertion (jwt)
    DecodedJWT decodedJwt = JWT.decode(clientAssertion);

    HapiReferenceServerProperties properties = new HapiReferenceServerProperties();
    String clientId = properties.getBulkClientId();
    if (!clientId.equals(decodedJwt.getIssuer())) {
      throw new OAuth2Exception(ErrorCode.INVALID_GRANT,
          "Issuer should be " + clientId);
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
      throw new OAuth2Exception(ErrorCode.INVALID_SCOPE, message);
    }
  }

  /**
   * Provide a code to get a bearer token for authorization.
   *
   * @param code code to get token
   * @return bearer token to be used for authorization
   */
  @PostMapping(path = "/token", produces = {"application/json"})
  public ResponseEntity<String> getToken(
      @RequestParam(name = "code", required = false) String code,
      @RequestParam(name = "client_id", required = false) String clientIdRequestParam,
      @RequestParam(name = "refresh_token", required = false) String refreshTokenValue,
      @RequestParam(name = "code_verifier", required = false) String codeVerifier,
      @RequestParam(name = "grant_type", required = false) String grantType,
      @RequestParam(name = "scope", required = false) String scopes,
      @RequestParam(name = "client_assertion_type", required = false) String clientAssertionType,
      @RequestParam(name = "client_assertion", required = false) String clientAssertion,
      HttpServletRequest request
  ) {

    Log.info("code is " + code);
    if (grantType == null) {
      throw new OAuth2Exception(ErrorCode.INVALID_REQUEST, "No grant_type provided.");
    }

    if (CLIENT_CREDENTIALS_GRANT_TYPE.equals(grantType)) {
      return getTokenByBackendServiceAuthorization(
                                                   scopes,
                                                   clientAssertionType,
                                                   clientAssertion,
                                                   request
                                                  );
    } else if (!(AUTHORIZATION_CODE_GRANT_TYPE.equals(grantType)
              || REFRESH_TOKEN_GRANT_TYPE.equals(grantType))) {
      throw new OAuth2Exception(ErrorCode.UNSUPPORTED_GRANT_TYPE, "Bad Grant Type: " + grantType);
    }

    String clientId =
        validateClient(request, clientIdRequestParam, clientAssertionType, clientAssertion);

    if (code != null) {
      return validateCode(request, code, clientId, codeVerifier);
    } else if (refreshTokenValue != null) {

      try {
        if (TokenManager.getInstance().authenticateRefreshToken(refreshTokenValue)) {
          Token refreshToken = TokenManager.getInstance().getRefreshToken(refreshTokenValue);
          String patientId = refreshToken.getPatientId();
          String encounterId = refreshToken.getEncounterId();
          String refreshTokenScopes = refreshToken.getScopesString();
          return generateBearerTokenResponse(request, clientId, refreshTokenScopes, patientId,
           encounterId);
        }
      } catch (TokenNotFoundException | InvalidBearerTokenException tokenNotFoundException) {
        throw new OAuth2Exception(ErrorCode.INVALID_GRANT,
            "Refresh Token " + refreshTokenValue + " was not found");
      }
    }
    throw new OAuth2Exception(ErrorCode.INVALID_REQUEST, "No code or refresh token provided.");
  }

  private static String validateClient(HttpServletRequest request, String clientIdRequestParam,
      String clientAssertionType, String clientAssertion) {
    String clientId;
    String clientSecret = null;
    String basicHeader = null;

    if (clientAssertionType == null) {
      // check client id and client secret if the server is confidential
      basicHeader = getBasicHeader(request);

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
    } else if (JWT_BEARER_CLIENT_ASSERTION_TYPE.equals(clientAssertionType)) {
      // confidential asymmetric
      DecodedJWT decodedJwt = JWT.decode(clientAssertion);
      clientId = decodedJwt.getIssuer();

      try {
        // In this case we cache the JWKS file locally, but
        // this verification is normally done against the registered JWKS for the given client, eg:
        // JwkProvider provider = new UrlJwkProvider("https://inferno.healthit.gov/suites/custom/smart_stu2/");
        HapiReferenceServerProperties properties = new HapiReferenceServerProperties();
        URL jwks = AuthorizationController.class.getResource(properties.getAsymmetricClientJwks());
        JwkProvider provider = new UrlJwkProvider(jwks);
        Jwk jwk = provider.get(decodedJwt.getKeyId());
        Algorithm algorithm;
        if (decodedJwt.getAlgorithm().equals("RS384")) {
          algorithm = Algorithm.RSA384((RSAPublicKey) jwk.getPublicKey(), null);
        } else if (decodedJwt.getAlgorithm().equals("ES384")) {
          algorithm = Algorithm.ECDSA384((ECPublicKey) jwk.getPublicKey(), null);
        } else {
          // the above are the only 2 options supported in the SMART app launch test kit.
          // if more are added, report support for them in WellKnownAuthorizationEndpointController
          throw new OAuth2Exception(ErrorCode.INVALID_REQUEST,
              "Unsupported encryption method " + decodedJwt.getAlgorithm());
        }

        algorithm.verify(decodedJwt);
      } catch (SignatureVerificationException e) {
        throw new OAuth2Exception(ErrorCode.INVALID_GRANT,
          "Client Assertion JWT failed signature verification", e);
      } catch (SigningKeyNotFoundException e) {
        // thrown by provider.get(jwt.kid) above
        throw new OAuth2Exception(ErrorCode.INVALID_REQUEST,
          "No key found with kid " + decodedJwt.getKeyId(), e);
      } catch (InvalidPublicKeyException e) {
        // thrown by jwk.getPublicKey above, should never happen
        throw new OAuth2Exception(ErrorCode.SERVER_ERROR, "Failed to parse public key", e)
            .withResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
      } catch (JwkException e) {
        // thrown by provider.get(jwt.kid) above,
        // shouldn't be possible in practice as the method only throws
        //  the more specific SigningKeyNotFound
        throw new OAuth2Exception(ErrorCode.SERVER_ERROR, "Unknown error occurred", e)
          .withResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
      }

    } else {
      throw new OAuth2Exception(ErrorCode.INVALID_REQUEST,
          "Unexpected Client Assertion Type: " + clientAssertionType);
    }

    authorizeClientId(clientId, basicHeader != null);
    authenticateClient(clientId, basicHeader, clientAssertionType, clientAssertion, clientSecret);

    return clientId;
  }

  private ResponseEntity<String> validateCode(
      HttpServletRequest request,
      String encodedCodeString,
      String clientId,
      String codeVerifier
  ) {
    try {
      String rawCodeString = new String(Base64.getDecoder().decode(encodedCodeString));
      JSONObject codeObject = new JSONObject(rawCodeString);

      String scopes = null;
      if (codeObject.has("scopes")) {
        scopes = (String) codeObject.get("scopes");
      }

      String patientId = null;
      if (codeObject.has("patientId")) {
        patientId = (String) codeObject.get("patientId");
      }

      String encounterId = null;
      if (codeObject.has("encounterId")) {
        encounterId = (String) codeObject.get("encounterId");
      }

      String codeChallenge = null;
      if (codeObject.has("codeChallenge")) {
        codeChallenge = (String) codeObject.get("codeChallenge");
      }

      String codeChallengeMethod = null;
      if (codeObject.has("codeChallengeMethod")) {
        codeChallengeMethod = (String) codeObject.get("codeChallengeMethod");
      }

      String code = null;
      if (codeObject.has("code")) {
        code = (String) codeObject.get("code");
      }

      validatePkce(codeChallengeMethod, codeChallenge, codeVerifier, scopes);

      if (code != null && FhirReferenceServerUtils.SAMPLE_CODE.equals(code)) {
        return generateBearerTokenResponse(request, clientId, scopes, patientId, encounterId);
      }
    } catch (IllegalArgumentException exception) {
      throw new OAuth2Exception(ErrorCode.INVALID_GRANT, "Invalid code");
    }

    throw new OAuth2Exception(ErrorCode.INVALID_GRANT, "Invalid code");
  }

  private void validatePkce(
        String codeChallengeMethod,
        String codeChallenge,
        String codeVerifier,
        String scopes
  ) {
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
      throw new OAuth2Exception(ErrorCode.INVALID_GRANT,
          "Only S256 PKCE code challenge method is supported"
      );
    }

    if (codeChallenge == null) {
      throw new OAuth2Exception(ErrorCode.INVALID_GRANT, "No code challenge received");
    }

    if (codeVerifier == null) {
      throw new OAuth2Exception(ErrorCode.INVALID_GRANT, "No code verifier received");
    }

    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] rawHash = digest.digest(codeVerifier.getBytes(StandardCharsets.UTF_8));
      String hash = Base64.getUrlEncoder().withoutPadding().encodeToString(rawHash);

      if (!codeChallenge.equalsIgnoreCase(hash)) {
        throw new OAuth2Exception(ErrorCode.INVALID_GRANT, "Invalid code verifier");
      }
    } catch (NoSuchAlgorithmException exception) {
      // Thrown by MessageDigest.getInstance("SHA-256"), should not be reachable
      throw new OAuth2Exception(ErrorCode.SERVER_ERROR, "Unable to process code verifier")
        .withResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return;
  }

  private ResponseEntity<String> generateBearerTokenResponse(HttpServletRequest request,
      String clientId, String scopes, String patientId, String encounterId) {
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
   */
  private String generateBearerToken(HttpServletRequest request, String clientId, String scopes,
      String patientId, String encounterId) {
    Long expiresIn = 3600L;
    TokenManager tokenManager = TokenManager.getInstance();
    Token token = tokenManager.createToken(scopes);
    token.setClientId(clientId);
    token.setPatientId(patientId);
    token.setEncounterId(encounterId);
    token.setExp(java.time.Instant.now().getEpochSecond() + expiresIn);

    String refreshTokenValue;
    try {
      Token refreshToken = tokenManager.getCorrespondingRefreshToken(token.getTokenValue());
      refreshToken.setPatientId(patientId);
      refreshToken.setEncounterId(encounterId);
      refreshToken.setClientId(clientId);
      refreshTokenValue = refreshToken.getTokenValue();
    } catch (TokenNotFoundException e) {
      throw new OAuth2Exception(ErrorCode.INVALID_GRANT, e);
    }

    List<String> scopesList = FhirReferenceServerUtils.getScopesListByScopeString(scopes);
    scopesList = processScopes(scopesList);
    scopes = FhirReferenceServerUtils.getScopesStringFromScopesList(scopesList);

    String accessToken = token.getTokenValue();

    JSONObject tokenJson = new JSONObject();
    tokenJson.put("access_token", accessToken);
    tokenJson.put("token_type", "bearer");
    tokenJson.put("expires_in", expiresIn);
    tokenJson.put("refresh_token", refreshTokenValue);
    tokenJson.put("scope", scopes);
    tokenJson.put("smart_style_url", FhirReferenceServerUtils.getSmartStyleUrl(request));
    tokenJson.put("need_patient_banner", false);

    if ("".equals(patientId) || patientId == null) {
      throw new OAuth2Exception(ErrorCode.UNAUTHORIZED_CLIENT, "No patients found")
        .withResponseStatus(HttpStatus.UNAUTHORIZED);
    }

    if (scopesList.contains("launch") || scopesList.contains("launch/patient")) {
      tokenJson.put("patient", patientId);
    }

    if (scopesList.contains("launch") || scopesList.contains("launch/encounter")) {
      if (Objects.equals(encounterId, "") || encounterId == null) {
        IGenericClient client = FhirReferenceServerUtils.getClientFromRequest(request);

        Encounter encounter = getFirstEncounterByPatientId(client, patientId);

        if (encounter == null) {
          throw new OAuth2Exception(ErrorCode.UNAUTHORIZED_CLIENT, "No encounters found")
            .withResponseStatus(HttpStatus.UNAUTHORIZED);
        }

        encounterId = encounter.getIdElement().getIdPart();
      }

      tokenJson.put("encounter", encounterId);
    }

    if (scopesList.contains("openid")
        && (scopesList.contains("fhirUser") || scopesList.contains("profile"))) {
      try {
        tokenJson.put("id_token", generateSampleOpenIdToken(request, clientId, patientId));
      } catch (OpenIdTokenGenerationException openIdTokenGenerationException) {
        throw new OAuth2Exception(ErrorCode.SERVER_ERROR, openIdTokenGenerationException)
            .withResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }
    return tokenJson.toString();
  }

  /**
   * Helper class used only as the structure to send info about scopes to the UI.
   */
  protected static class ScopeWrapper {
    // fields are public so the default JSON serializer picks them up
    public String v1;
    public String v2;
    public List<String> subscopes = new ArrayList<>();
  }

  /**
   * Get the set of scopes to offer to an authorizing user, based on the scopes they requested.
   * Invalid scopes will not be returned, and if a resource-level scope is requested
   * then granular subscopes will be included as well.
   * The output format is a Map of { scope: [subscope, ...] }
   *
   * @param requestScopes String of scopes, space-separated
   * @param request The request
   * @return Scopes that the user may authorize
   */
  @PostMapping(path = "/supportedScopes", produces = {"application/json"})
  static ResponseEntity<List<ScopeWrapper>> supportedScopes(
      @RequestBody Object requestScopes, HttpServletRequest request) {
    if (!(requestScopes instanceof String)) {
      // not sure this is possible, just being defensive
      return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    List<String> scopesList =
        FhirReferenceServerUtils.getScopesListByScopeString(requestScopes.toString());

    List<ScopeWrapper> scopesOut = new ArrayList<>();
    for (String s : scopesList) {
      Scope scope = Scope.fromString(s);
      if (scope == null) {
        // Don't return known-invalid scopes
        continue;
      }
      ScopeWrapper wrapper = new ScopeWrapper();
      scopesOut.add(wrapper);
      if (scope.version == 2) {
        wrapper.v2 = s;
      } else {
        wrapper.v1 = s;
        wrapper.v2 = scope.asVersion2().toString();
      }

      if (scope.resourceType != null && scope.parameters == null
          && KNOWN_PARAMS.containsKey(scope.resourceType)) {
        // note the parameters null check - don't add the param to an already-granular scope
        for (String p : KNOWN_PARAMS.get(scope.resourceType)) {
          String newScope = Scope.fromString(s + "?" + p).asVersion2().toString();
          wrapper.subscopes.add(newScope);
        }
      }
    }

    return new ResponseEntity<>(scopesOut, HttpStatus.OK);
  }

  /**
   * Process the list of scopes and apply any custom logic, eg filtering, consolidating, etc.
   */
  static List<String> processScopes(List<String> scopeStrings) {
    // First pass: parse the scope strings into Scope objects,
    // remove scopes that we recognize as invalid,
    // note if there are any v2 scopes
    List<Scope> scopes = new ArrayList<Scope>(scopeStrings.size());

    boolean hasV2Scope = false;

    for (String scopeString : scopeStrings) {
      Scope scope = Scope.fromString(scopeString);
      if (scope == null) {
        // scope.isValid() returned false so Scope.fromString returned null.
        // Remove this scope from the list
        continue;
      }

      scopes.add(scope);

      if (scope.version == 2 || (scope.parameters != null && !scope.parameters.isEmpty())) {
        // checking for parameters here makes things easier in the auth UI,
        // we can just append parameters to any scope rather than parsing out the privileges
        hasV2Scope = true;
      }
    }

    // Second pass: if there are v2 scopes, make sure all scopes are v2
    if (hasV2Scope) {
      for (int i = 0; i < scopes.size(); i++) {
        scopes.set(i, scopes.get(i).asVersion2());
      }
    }

    return scopes.stream().map(Scope::toString).toList();
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
          .withSubject(TokenManager.SUB_STRING).withAudience(clientId)
          .withExpiresAt(expiresAt).withIssuedAt(issuedAt)
          .withClaim("fhirUser", fhirUserUrl).sign(algorithm);
    } catch (RsaKeyException rsaKeyException) {
      throw new OpenIdTokenGenerationException(rsaKeyException);
    }
  }

  /**
   * Exception handler for OAuth2Exception and subclasses.
   * Errors in the access token flow are rendered as JSON with an "error"
   * and optionally "error_description" field, per RFC 6749, section 5.2
   * https://datatracker.ietf.org/doc/html/rfc6749#section-5.2
   * @param ex The raised exception
   * @return error response content
   */
  @ExceptionHandler(OAuth2Exception.class)
  public ResponseEntity<Map<String, String>> handleOAuth2Exception(OAuth2Exception ex) {
    Map<String, String> json = new HashMap<>();
    json.put("error", ex.getError());
    if (ex.getErrorDescription() != null) {
      json.put("error_description", ex.getErrorDescription());
    }
    return ResponseEntity
              .status(ex.getResponseStatus())
              .contentType(MediaType.APPLICATION_JSON)
              .headers(ex.getResponseHeaders())
              .body(json);
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

  private static void authorizeClientId(String clientId, boolean basicAuth) {
    HapiReferenceServerProperties properties = new HapiReferenceServerProperties();
    if (!properties.getPublicClientId().equals(clientId)
        && !properties.getConfidentialClientId().equals(clientId)
        && !properties.getAsymmetricClientId().equals(clientId)) {
      throw new InvalidClientIdException(clientId, basicAuth);
    }
  }

  /**
   * Authenticate the client based on the provided credentials,
   * enforcing the expected authentication mechanism per client.
   */
  private static void authenticateClient(String clientId, String basicHeader,
      String clientAssertionType, String clientAssertion, String clientSecret) {
    HapiReferenceServerProperties properties = new HapiReferenceServerProperties();

    // note we've called authorizeClientId already
    // so the only possible options here for clientId are our 3 predefined choices

    if (properties.getConfidentialClientId().equals(clientId)) {
      // confidential symmetric -- uses authorization header/basic auth
      if (basicHeader == null
          || !properties.getConfidentialClientSecret().equals(clientSecret)) {
        // Client Secret invalid or not supplied
        throw new InvalidClientSecretException();
      }
    } else if (properties.getAsymmetricClientId().equals(clientId)) {
      // confidential asymmetric -- uses a signed JWT in the client_assertion

      // actual validation of the client assertion was done previously
      // if it was provided, so here just check that it was provided
      if (!JWT_BEARER_CLIENT_ASSERTION_TYPE.equals(clientAssertionType)
          || clientAssertion == null || clientAssertion.isBlank()) {
        throw new OAuth2Exception(ErrorCode.INVALID_CLIENT,
            "Client assertion invalid or not supplied")
            .withResponseStatus(HttpStatus.UNAUTHORIZED);
      }
    } else if (properties.getPublicClientId().equals(clientId)) {
      // public app; safeguarding secrets is not possible
      // and so credentials should not be provided

      if (basicHeader != null || clientAssertionType != null) {
        throw new OAuth2Exception(ErrorCode.INVALID_CLIENT,
            "Public clients may not provide secrets or assertions")
            .withResponseStatus(HttpStatus.UNAUTHORIZED);
      }
    }
  }
}
