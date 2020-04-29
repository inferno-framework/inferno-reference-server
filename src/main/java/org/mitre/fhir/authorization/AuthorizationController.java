package org.mitre.fhir.authorization;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.dnault.xmlpatch.internal.Log;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.json.JSONObject;
import org.mitre.fhir.authorization.exception.BearerTokenException;
import org.mitre.fhir.authorization.exception.InvalidClientIdException;
import org.mitre.fhir.authorization.exception.InvalidClientSecretException;
import org.mitre.fhir.authorization.exception.OpenIdTokenGenerationException;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.FhirUtils;
import org.mitre.fhir.utils.RsaUtils;
import org.mitre.fhir.utils.exception.RSAKeyException;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.*;
import java.util.Base64.Decoder;

@RestController
public class AuthorizationController {

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
    String decodedValue = new String(decoder.decode(encodedValue));
    return decodedValue;
  }

  private static void authorizeClientId(String clientId) {
    if (!FhirReferenceServerUtils.SAMPLE_PUBLIC_CLIENT_ID.equals(clientId)
        && !FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_ID.equals(clientId)) {
      throw new InvalidClientIdException(clientId);
    }
  }

  private static void authenticateClientIdAndClientSecret(String clientId, String clientSecret) {

    authorizeClientId(clientId);

    if (FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_ID.equals(clientId)
        && !FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_SECRET.equals(clientSecret)) {
      throw new InvalidClientSecretException();
    }
  }

  @PostConstruct
  protected void postConstruct() {
    Log.info("Authorization Controller added");
  }

  @GetMapping(path = "authorizeClientId/{clientId}", produces = {"application/json"})
  public String validateClientId(@PathVariable String clientId, HttpServletRequest request) {
    authorizeClientId(clientId);
    String fhirServerBaseUrl = FhirReferenceServerUtils.getServerBaseUrl(request)
        + FhirReferenceServerUtils.FHIR_SERVER_PATH;
    FhirContext fhirContext = FhirContext.forR4();
    IGenericClient client = fhirContext.newRestfulGenericClient(fhirServerBaseUrl);

    Bundle patientsBundle = FhirUtils.getPatientsBundle(client);
    String json = fhirContext.newJsonParser().encodeResourceToString(patientsBundle);


    return json;
  }

  /**
   * Provide a code to get a bearer token for authorization
   *
   * @param code
   * @return bearer token to be used for authorization
   * @throws BearerTokenException
   */
  @PostMapping(path = "/token", produces = {"application/json"})
  public ResponseEntity<String> getToken(@RequestParam(name = "code", required = false) String code,
                                         @RequestParam(name = "client_id", required = false) String clientIdRequestParam,
                                         @RequestParam(name = "refresh_token", required = false) String refreshToken, HttpServletRequest request) throws BearerTokenException {

    Log.info("code is " + code);

    // check client id and client secret if the server is confidential
    String basicHeader = getBasicHeader(request);

    String clientId = null;
    String clientSecret = null;

    // if basic header exists, extract clientId and clientSecret from basic header
    if (basicHeader != null) {
      String decodedValue = getDecodedBasicAuthorizationString(basicHeader);
      clientId = decodedValue.split(":")[0]; // client id is user name, and should be before ':'
      clientSecret = decodedValue.split(":")[1]; // client secret is password, and should be after ':'
    }

    // if no basic auth, client id should be supplied as request param
    else {
      clientId = clientIdRequestParam;
    }

    authenticateClientIdAndClientSecret(clientId, clientSecret);

    String scopes = "";
    String patientId = "";

    String fullCodeString;

    if (code != null) {
      fullCodeString = code;
    } else if (refreshToken != null) {
      fullCodeString = refreshToken;
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
    }

    // the provided code is actualcode.scopes
    String[] fullCode = fullCodeString.split("\\.");

    String actualCodeOrRefreshToken = fullCode[0];

    // if scope was included
    if (fullCode.length >= 2) {
      String encodedScopes = fullCode[1];
      scopes = new String(Base64.getDecoder().decode(encodedScopes));
    }

    if (fullCode.length >= 3) {
      String encodedPatientId = fullCode[2];
      patientId = new String(Base64.getDecoder().decode(encodedPatientId));
    }

    if ((code != null && FhirReferenceServerUtils.SAMPLE_CODE.equals(actualCodeOrRefreshToken)) || (refreshToken != null && FhirReferenceServerUtils.SAMPLE_REFRESH_TOKEN.equals(actualCodeOrRefreshToken))) {
      return generateBearerTokenResponse(request, clientId, scopes, patientId);
    }


    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid code");
  }

  private ResponseEntity<String> generateBearerTokenResponse(HttpServletRequest request, String clientId,
                                                             String scopes, String patientId) throws BearerTokenException {
    HttpHeaders headers = new HttpHeaders();
    headers.setCacheControl(CacheControl.noStore());
    headers.setPragma("no-cache");

    String tokenJSONString = generateBearerToken(request, clientId, scopes, patientId);

    ResponseEntity<String> responseEntity = new ResponseEntity<String>(tokenJSONString, headers, HttpStatus.OK);

    return responseEntity;
  }

  /**
   * Generates Token in Oauth2 expected format
   *
   * @return token JSON String
   * @throws BearerTokenException
   */
  private String generateBearerToken(HttpServletRequest request, String clientId, String scopes, String patientId) throws BearerTokenException {

    String fhirServerBaseUrl = FhirReferenceServerUtils.getServerBaseUrl(request)
        + FhirReferenceServerUtils.FHIR_SERVER_PATH;
    FhirContext fhirContext = FhirContext.forR4();
    IGenericClient client = fhirContext.newRestfulGenericClient(fhirServerBaseUrl);

    JSONObject tokenJSON = new JSONObject();

    String refreshToken = FhirReferenceServerUtils.createCode(FhirReferenceServerUtils.SAMPLE_REFRESH_TOKEN, scopes, patientId);

    List<String> scopesList = Arrays.asList(scopes.split(" "));

    String encodedScopes = Base64.getEncoder().encodeToString(scopes.getBytes());

    String accessToken = FhirReferenceServerUtils.SAMPLE_ACCESS_TOKEN + "." + encodedScopes;

    tokenJSON.put("access_token", accessToken);
    tokenJSON.put("token_type", "bearer");
    tokenJSON.put("expires_in", 3600);
    tokenJSON.put("refresh_token", refreshToken);
    tokenJSON.put("scope", scopes);
    tokenJSON.put("smart_style_url", FhirReferenceServerUtils.getSmartStyleUrl(request));
    tokenJSON.put("need_patient_banner", false);

    if ("".equals(patientId)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No patients found");
    }

    // get their id
    //String patientId = patient.getIdElement().getIdPart();

    if (scopesList.contains("launch") || scopesList.contains("launch/patient")) {
      tokenJSON.put("patient", patientId);
    }

    if (scopesList.contains("launch") || scopesList.contains("launch/encounter")) {
      Encounter encounter = getFirstEncounterByPatientId(client, patientId);

      if (encounter == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "No encounters found");
      }

      String encounterId = encounter.getIdElement().getIdPart();
      tokenJSON.put("encounter", encounterId);
    }

    try {
      tokenJSON.put("id_token", generateSampleOpenIdToken(request, clientId, patientId));
    } catch (OpenIdTokenGenerationException openIdTokenGenerationException) {
      throw new BearerTokenException(openIdTokenGenerationException);
    }
    return tokenJSON.toString();
  }

  /**
   * Generates a sample open id token
   * https://openid.net/specs/openid-connect-core-1_0.html
   *
   * @return token JSON String representing the open id token
   * @throws OpenIdTokenGenerationException
   */
  private String generateSampleOpenIdToken(HttpServletRequest request, String clientId, String patientId) throws OpenIdTokenGenerationException {

    try {
      RSAPublicKey publicKey = RsaUtils.getRsaPublicKey();
      RSAPrivateKey privateKey = RsaUtils.getRsaPrivateKey();

      // for now hard coding as a Patient
      // http://hl7.org/fhir/smart-app-launch/worked_example_id_token/index.html#Encode-them-in-a-JWT
      String fhirUserURL = FhirReferenceServerUtils.getFhirServerBaseUrl(request) + "/Patient/" + patientId;

      Calendar calendar = Calendar.getInstance();

      Date issuedAt = calendar.getTime();
      calendar.set(Calendar.YEAR, calendar.get(Calendar.YEAR) + 1);
      Date expiresAt = calendar.getTime();


      Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
      String token = JWT.create()
          .withIssuer(FhirReferenceServerUtils.getFhirServerBaseUrl(request))
          .withSubject("")
          .withAudience(clientId)
          .withExpiresAt(expiresAt)
          .withIssuedAt(issuedAt)
          .withClaim("fhirUser", fhirUserURL).sign(algorithm);

      return token;
    } catch (RSAKeyException rsaKeyException) {
      throw new OpenIdTokenGenerationException(rsaKeyException);
    }
  }

  private Encounter getFirstEncounterByPatientId(IGenericClient client, String patientId) {
    Encounter encounter = null;

    Bundle encountersBundle = client.search().forResource(Encounter.class).where(Encounter.PATIENT.hasId(patientId)).returnBundle(Bundle.class)
        .cacheControl(new CacheControlDirective().setNoCache(true))
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(FhirReferenceServerUtils.SAMPLE_ACCESS_TOKEN, FhirReferenceServerUtils.DEFAULT_SCOPE))
        .execute();
    List<BundleEntryComponent> encounters = encountersBundle.getEntry();

    for (BundleEntryComponent bundleEntryComponent : encounters) {
      if (bundleEntryComponent.getResource().fhirType().equals("Encounter")) {
        encounter = (Encounter) bundleEntryComponent.getResource();
        break;
      }
    }

    return encounter;
  }

}
