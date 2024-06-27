package org.mitre.fhir.authorization;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mitre.fhir.utils.FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mitre.fhir.authorization.exception.InvalidClientIdException;
import org.mitre.fhir.authorization.exception.InvalidClientSecretException;
import org.mitre.fhir.authorization.exception.OAuth2Exception;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.FhirUtils;
import org.mitre.fhir.utils.RsaUtils;
import org.mitre.fhir.utils.TestUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestAuthorization {

  private static IGenericClient ourClient;
  private static FhirContext ourCtx;
  private static int ourPort;
  private static Server ourServer;
  private static String ourServerBase;

  private static IIdType testPatientId;
  private static IIdType testEncounterId;

  private static Token testToken;

  private static final String SAMPLE_CODE = "SAMPLE_CODE";

  private static final String SAMPLE_PUBLIC_CLIENT_ID = "SAMPLE_PUBLIC_CLIENT_ID";
  private static final String SAMPLE_CONFIDENTIAL_CLIENT_ID = "SAMPLE_CONFIDENTIAL_CLIENT_ID";
  private static final String SAMPLE_CONFIDENTIAL_CLIENT_SECRET =
      "SAMPLE_CONFIDENTIAL_CLIENT_SECRET";

  @Test
  public void testCreateAndRead() {
    String methodName = "testCreateResourceConditional";

    Patient pt = new Patient();
    pt.addName().setFamily(methodName);
    IIdType id = ourClient.create().resource(pt)
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();

    Patient pt2 = ourClient.read().resource(Patient.class).withId(id)
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();
    Assert.assertEquals(methodName, pt2.getName().get(0).getFamily());

    // delete the new entry so the db won't have a leftover artifact
    ourClient.delete().resourceById(id)
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();
  }

  @Test(expected = AuthenticationException.class)
  public void testInterceptor() {
    String methodName = "testCreateResourceConditional";

    // with no header, will fail
    Patient pt = new Patient();
    pt.addName().setFamily(methodName);

    ourClient.create().resource(pt).execute();
  }

  @Test(expected = AuthenticationException.class)
  public void testInterceptorScopes() {

    String scope = "launch/patient patient/Patient.read patient/Condition patient/Observation.read";
    Token token = TokenManager.getInstance().createToken(scope);

    ourClient.search().forResource("Patient")
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();

    ourClient.search().forResource("Condition")
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();

    ourClient.search().forResource("Observation")
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();

    ourClient.search().forResource("AllergyIntolerance")
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();
  }

  @Test
  public void testInterceptorWithStarScopes() {
    String scope = "patient/*.*";
    Token token = TokenManager.getInstance().createToken(scope);

    ourClient.search().forResource("Patient")
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();

    ourClient.search().forResource("Condition")
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();

    ourClient.search().forResource("Observation")
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();
  }

  @Test(expected = AuthenticationException.class)
  public void testIntercepterWithoutToken() {
    ourClient.search().forResource("Patient").execute();
  }

  @Test
  public void testTestAuthorizationWithInvalidCode() throws Exception {
    try {
      String serverBaseUrl = "";
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setLocalAddr("localhost");
      request.setRequestURI(serverBaseUrl);
      request.setServerPort(TestUtils.TEST_PORT);

      AuthorizationController authorizationController = new AuthorizationController();
      authorizationController.getToken("{\"code\":\"INVALID_CODE\"}", null, null, null,
          "authorization_code", null, null, null, request);
      Assert.fail("Did not get expected Unauthorized ResponseStatusException");
    } catch (OAuth2Exception rse) {
      if (!HttpStatus.UNAUTHORIZED.equals(rse.getResponseStatus())) {
        throw rse;
      }
    }
  }

  @Test
  public void testTestAuthorizationWithNullCode() throws Exception {
    try {
      String serverBaseUrl = "";
      MockHttpServletRequest request = new MockHttpServletRequest();
      request.setLocalAddr("localhost");
      request.setRequestURI(serverBaseUrl);
      request.setServerPort(TestUtils.TEST_PORT);

      AuthorizationController authorizationController = new AuthorizationController();
      authorizationController.getToken(null, null, "SAMPLE_CLIENT_ID", null, "authorization_code",
          null, null, null, request);
      Assert.fail("Did not get expected Unauthorized ResponseStatusException");
    } catch (OAuth2Exception rse) {
      if (!HttpStatus.UNAUTHORIZED.equals(rse.getResponseStatus())) {
        throw rse;
      }
    }
  }

  @Test
  public void testTestAuthorizationWithPublicClient() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization",
        TestUtils.getEncodedBasicAuthorizationHeaderWithPublicClient());

    String scopes = "launch/patient openId ";
    String code =
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart());

    AuthorizationController authorizationController = new AuthorizationController();
    ResponseEntity<String> tokenResponseEntity =
        authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, null,
            "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    Assert.assertNotNull(jsonNode.get("access_token"));
  }

  @Test
  public void testTestAuthorizationWithValidCode() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId ";
    String code =
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart());

    AuthorizationController authorizationController = new AuthorizationController();
    ResponseEntity<String> tokenResponseEntity =
        authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, null,
            "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    Assert.assertNotNull(jsonNode.get("access_token"));
  }

  @Test
  public void testReadScopeNoScopeProvided() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String scopes = "";
    AuthorizationController authorizationController = new AuthorizationController();
    ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
        "SAMPLE_PUBLIC_CLIENT_ID", null, null, "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    String scope = jsonNode.get("scope").asText();

    Assert.assertEquals("", scope);
  }

  @Test
  public void testReadScope() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String scopes = "/patient openId _-\\/";

    AuthorizationController authorizationController = new AuthorizationController();
    ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
        "SAMPLE_PUBLIC_CLIENT_ID", null, null, "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    String scopeResult = jsonNode.get("scope").asText();

    Assert.assertEquals(scopes, scopeResult);
  }

  @Test
  public void testCapabilityStatementNotBlockedByInterceptor() {
    // should throw an exception if intercepter does not white list it
    ourClient.capabilities().ofType(CapabilityStatement.class).execute();
  }

  @Test
  public void testCapabilityStatementListResourceIsCorrect() {
    // Get the CapabilityStatement
    CapabilityStatement capabilityStatement =
        ourClient.capabilities().ofType(CapabilityStatement.class).execute();
    capabilityStatement.getRest().get(0).getResource().stream()
        .filter(restResource -> "List".equals(restResource.getType())).findFirst()
        .ifPresent(listResource -> Assert.assertEquals(listResource.getProfile(),
            "http://hl7.org/fhir/StructureDefinition/List"));
  }

  @Test
  public void testCapabilityStatementOauthUris() {
    CapabilityStatement capabilityStatement =
        ourClient.capabilities().ofType(CapabilityStatement.class).execute();
    CapabilityStatementRestComponent rest = capabilityStatement.getRest().get(0);
    CapabilityStatementRestSecurityComponent security = rest.getSecurity();
    List<Extension> extensions = security.getExtension();

    Extension oauthUrisExtension = extensions.get(0);

    List<Extension> oauthUrisExtensionExtensions = oauthUrisExtension.getExtension();

    Map<String, String> oauthUriMap = new HashMap<>();
    for (Extension extension : oauthUrisExtensionExtensions) {
      oauthUriMap.put(extension.getUrl(), extension.getValue().primitiveValue());
    }

    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String authorizeUri =
        oauthUriMap.get(ServerConformanceWithAuthorizationProvider.AUTHORIZE_EXTENSION_URL);
    String revokeUri =
        oauthUriMap.get(ServerConformanceWithAuthorizationProvider.REVOKE_EXTENSION_URL);
    String tokenUri =
        oauthUriMap.get(ServerConformanceWithAuthorizationProvider.TOKEN_EXTENSION_URL);

    Assert.assertEquals(authorizeUri,
        ServerConformanceWithAuthorizationProvider.getAuthorizationExtensionUri(request));
    Assert.assertEquals(revokeUri,
        ServerConformanceWithAuthorizationProvider.getRevokeExtensionUri(request));
    Assert.assertEquals(tokenUri,
        ServerConformanceWithAuthorizationProvider.getTokenExtensionUri(request));
  }


  @Test
  public void testGetTokenWithoutBasicAuth() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String scopes = "";
    AuthorizationController authorizationController = new AuthorizationController();
    // shouldn't throw an exception
    authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
        SAMPLE_PUBLIC_CLIENT_ID, null, null, "authorization_code", null, null, null, request);
  }

  @Test(expected = InvalidClientIdException.class)
  public void testGetTokenWithoutBasicAuthAndInvalidClientId() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, "INVALID_CLIENT_ID",
                                     null, null, "authorization_code", null, null, null, request);
  }

  @Test(expected = InvalidClientIdException.class)
  public void testGetTokenWithoutBasicAuthAndNullClientId() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, null,
        "authorization_code", null, null, null, request);
  }

  @Test
  public void testGetTokenWithBasicAuth() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, "", testPatientId.getIdPart()),
        null,
        null,
        null,
        "authorization_code",
        null,
        null,
        null,
        request
    );
  }

  @Test(expected = OAuth2Exception.class)
  public void testGetTokenNoPatientScopeProvided() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, null, null),
        null,
        null,
        null,
        "authorization_code",
        null,
        null,
        null,
        request
    );
  }

  @Test
  public void testGetTokenNoEncounterScopeProvided() throws Exception {
    AuthorizationController authorizationController = new AuthorizationController();
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String scopes = "launch/patient openId ";

    String code =
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart());

    ResponseEntity<String> tokenResponseEntity =
        authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, null,
            "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    JsonNode patientId = jsonNode.get("patient");
    JsonNode encounterId = jsonNode.get("encounter");

    Assert.assertNotNull(patientId);
    Assert.assertNull(encounterId);
  }

  @Test
  public void testGetTokenNoPatientOrEncounterScopeProvided() throws Exception {
    AuthorizationController authorizationController = new AuthorizationController();
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String scopes = "";

    String code =
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart());

    ResponseEntity<String> tokenResponseEntity =
        authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, null,
            "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    JsonNode patientId = jsonNode.get("patient");
    JsonNode encounterId = jsonNode.get("encounter");

    Assert.assertNull(patientId);
    Assert.assertNull(encounterId);
  }

  @Test
  public void testPatientAndEncounterScopeProvided() throws Exception {
    AuthorizationController authorizationController = new AuthorizationController();
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String scopes = "launch/patient launch/encounter ";

    ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
        "SAMPLE_PUBLIC_CLIENT_ID", null, null, "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    String patientId = jsonNode.get("patient").asText();
    String encounterId = jsonNode.get("encounter").asText();

    Assert.assertNotNull(patientId);
    Assert.assertNotNull(encounterId);
  }

  @Test
  public void testNoPatientScopeButEncounterScopeProvided() throws Exception {
    AuthorizationController authorizationController = new AuthorizationController();
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String scopes = "launch/encounter ";

    ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
        "SAMPLE_PUBLIC_CLIENT_ID", null, null, "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    JsonNode patientId = jsonNode.get("patient");
    JsonNode encounterId = jsonNode.get("encounter");

    Assert.assertNull(patientId);
    Assert.assertNotNull(encounterId);
  }

  @Test(expected = InvalidClientIdException.class)
  public void testGetTokenWithBasicAuthWithInvalidClientId() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader(
        "INVALID_CLIENT_ID", SAMPLE_CONFIDENTIAL_CLIENT_SECRET));
    String encodedScopes = "";
    String code = FhirReferenceServerUtils.SAMPLE_CODE + encodedScopes;

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(code, null, null, null, "authorization_code", null, null, null,
        request);
  }

  @Test
  public void testGetTokenWithBasicAuthWithConfidentialClientId() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader(
        SAMPLE_CONFIDENTIAL_CLIENT_ID, SAMPLE_CONFIDENTIAL_CLIENT_SECRET));
    String scopes = "";

    AuthorizationController authorizationController = new AuthorizationController();
    // no error should be thrown
    authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()), null,
        null, null, "authorization_code", null, null, null, request);
  }

  @Test(expected = InvalidClientSecretException.class)
  public void testGetTokenWithBasicAuthWithInvalidClientSecret() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader(
        SAMPLE_CONFIDENTIAL_CLIENT_ID, "Invalid Client Secret"));

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, null,
        "authorization_code", null, null, null, request);
  }
  
  @Test
  public void testGetTokenWithhWithConfidentialAsymmetricClient() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String scopes = "";
    
    String asymmetricClientID = "SAMPLE_ASYMMETRIC_CLIENT_ID";
    // the key at src/test/resources/client_signing_key.key was generated 
    // via a JWK-to-PEM with the RSA private key from:
    // https://github.com/inferno-framework/smart-app-launch-test-kit/blob/main/lib/smart_app_launch/smart_jwks.json
    RSAPrivateKey privateKey = RsaUtils.getRsaPrivateKey("/client_signing_key.key");
    Algorithm algorithm = Algorithm.RSA384(null, privateKey);
    
    String clientAssertionType = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    String clientAssertion = JWT.create()
        .withIssuer(asymmetricClientID)
        .withSubject(asymmetricClientID)
        .withKeyId("b41528b6f37a9500edb8a905a595bdd7")
        .sign(algorithm);

    AuthorizationController authorizationController = new AuthorizationController();
    // no error should be thrown
    authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()), null,
        null, null, "authorization_code", null, clientAssertionType, clientAssertion, request);
  }

  @Test
  public void testGetTokenWithBasicAuthWithInvalidSignature() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);

    String asymmetricClientID = "SAMPLE_ASYMMETRIC_CLIENT_ID";
    // the private key of this server doesn't match the private key of the client
    // so signing with it should produce an error
    RSAPrivateKey privateKey = RsaUtils.getRsaPrivateKey();
    Algorithm algorithm = Algorithm.RSA384(null, privateKey);
    
    String clientAssertionType = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    String clientAssertion = JWT.create()
        .withIssuer(asymmetricClientID)
        .withSubject(asymmetricClientID)
        .withKeyId("b41528b6f37a9500edb8a905a595bdd7")
        .sign(algorithm);
    
    AuthorizationController authorizationController = new AuthorizationController();
    
    try {
      authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, null,
          "authorization_code", null, clientAssertionType, clientAssertion, request);
      Assert.fail("Token request should have thrown an exception for invalid signature");
    } catch (OAuth2Exception e) {
      Assert.assertTrue(e.getCause() instanceof SignatureVerificationException); 
    }
  }

  @Test
  public void testGetTokenGivesValidOpenId() throws Exception {
    AuthorizationController authorizationController = new AuthorizationController();
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "patient/Patient.rs openid fhirUser";
    ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()), null,
        null, null, "authorization_code", scopes, null, null, request);
    String jsonString = tokenResponseEntity.getBody();
    JSONObject jsonObject = new JSONObject(jsonString);
    String idToken = (String) jsonObject.get("id_token");

    // will throw an exception if invalid
    DecodedJWT decoded = JWT.decode(idToken);
    Algorithm algorithm = Algorithm.RSA256(RsaUtils.getRsaPublicKey(), null);

    JWT.require(algorithm).build().verify(decoded);

    Assert.assertEquals("RS256", decoded.getAlgorithm());
    Assert.assertNotNull(decoded.getClaim("fhirUser"));
    Assert.assertFalse(decoded.getSubject().isEmpty());
  }

  @Test
  public void testGetTokenOnlyReturnsOpenIdWithProperScope() throws Exception {
    AuthorizationController authorizationController = new AuthorizationController();
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "patient/Patient.r"; // no openid fhirUser scope requested
    ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()), null,
        null, null, "authorization_code", scopes, null, null, request);
    String jsonString = tokenResponseEntity.getBody();
    JSONObject jsonObject = new JSONObject(jsonString);

    Assert.assertFalse(jsonObject.has("id_token"));
  }

  @Test
  public void testTestAuthorizationWithRefreshToken() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    Token token = TokenManager.getInstance().getServerToken();
    Token refreshToken =
        TokenManager.getInstance().getCorrespondingRefreshToken(token.getTokenValue());
    refreshToken.setPatientId("SAMPLE_PATIENT_ID");

    String refreshTokenValue = refreshToken.getTokenValue();

    AuthorizationController authorizationController = new AuthorizationController();
    ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(
        null,
        "SAMPLE_PUBLIC_CLIENT_ID",
        refreshTokenValue,
        null,
        "authorization_code",
        null,
        null,
        null,
        request
    );

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    Assert.assertNotNull(jsonNode.get("access_token"));
  }

  @Test
  public void testUsingRefreshTokenReceivedFromToken() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId";
    String patientId = testPatientId.getIdPart();
    String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, patientId);

    AuthorizationController authorizationController = new AuthorizationController();

    ResponseEntity<String> tokenResponseEntity =
        authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, null,
            "authorization_code", null, null, null, request);

    ObjectMapper mapper = new ObjectMapper();

    String jsonString = tokenResponseEntity.getBody();

    JsonNode jsonNode = mapper.readTree(jsonString);
    String refreshToken = jsonNode.get("refresh_token").textValue();

    String serverBaseUrl2 = "";
    MockHttpServletRequest request2 = new MockHttpServletRequest();
    request2.setLocalAddr("localhost");
    request2.setRequestURI(serverBaseUrl2);
    request2.setServerPort(TestUtils.TEST_PORT);
    request2.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    // check that the new token has the same scopes and the new refresh token has the same patient
    // id
    ResponseEntity<String> newTokenResponseEntity =
        authorizationController.getToken(null, "SAMPLE_PUBLIC_CLIENT_ID", refreshToken, null,
            "authorization_code", null, null, null, request2);
    String newJsonString = newTokenResponseEntity.getBody();
    ObjectMapper mapper2 = new ObjectMapper();
    JsonNode newJsonNode = mapper2.readTree(newJsonString);
    String newScopes = newJsonNode.get("scope").textValue();

    Assert.assertEquals(newScopes, scopes);

    String newPatientId = newJsonNode.get("patient").textValue();

    Assert.assertEquals(newPatientId, patientId);
  }

  @Test(expected = OAuth2Exception.class)
  public void testTestAuthorizationNoCodeAndNoRefreshToken() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(null, "SAMPLE_PUBLIC_CLIENT_ID", null, null,
        "authorization_code", null, null, null, request);
  }

  @Test(expected = OAuth2Exception.class)
  public void testTestAuthorizationInvalidRefreshToken() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId ";
    String refreshToken = FhirReferenceServerUtils.createCode("INCORRECT_REFRESH_TOKEN", scopes,
        testPatientId.getIdPart());

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(null, "SAMPLE_PUBLIC_CLIENT_ID", refreshToken, null,
        "authorization_code", null, null, null, request);
  }

  @Test
  public void testGetAllPatientsBundle() {
    Bundle bundle = FhirUtils.getPatientsBundle(ourClient);
    List<BundleEntryComponent> bundleEntryComponents = bundle.getEntry();

    Assert.assertEquals(1, bundleEntryComponents.size());
  }

  @Test
  public void testSearch() {
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 1988);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 13);
    Date birthdate = cal.getTime();

    Token token = TokenManager.getInstance().getServerToken();

    Bundle results = ourClient.search().forResource(Patient.class)
        .where(Patient.BIRTHDATE.exactly().day(birthdate)).returnBundle(Bundle.class)
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(token.getTokenValue()))
        .execute();

    Assert.assertEquals(1, results.getTotal());
  }

  @Test
  public void testPkce() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId patient/*.read";
    String codeVerifier = UUID.randomUUID().toString() + UUID.randomUUID().toString();
    byte[] sha = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(UTF_8));
    String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha);
    String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes,
        testPatientId.getIdPart(), "s256", codeChallenge);

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, codeVerifier,
        "authorization_code", null, null, null, request);
  }

  @Test(expected = OAuth2Exception.class)
  public void testPkceRequiresValidCodeVerifier() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId patient/*.read";
    String codeVerifier = UUID.randomUUID().toString() + UUID.randomUUID().toString();
    byte[] sha = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(UTF_8));
    String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha);
    String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes,
        testPatientId.getIdPart(), "s256", codeChallenge);

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, codeVerifier + "X",
        "authorization_code", null, null, null, request);
  }

  @Test(expected = OAuth2Exception.class)
  public void testPkceRequiresCodeVerifier() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId patient/*.read";
    String codeVerifier = UUID.randomUUID().toString() + UUID.randomUUID().toString();
    byte[] sha = MessageDigest.getInstance("SHA-256").digest(codeVerifier.getBytes(UTF_8));
    String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(sha);
    String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes,
        testPatientId.getIdPart(), "s256", codeChallenge);

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, null,
        "authorization_code", null, null, null, request);
  }

  @Test(expected = OAuth2Exception.class)
  public void testPkceRequiresCodeChallenge() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId patient/*.read";
    String codeVerifier = UUID.randomUUID().toString() + UUID.randomUUID().toString();
    String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes,
        testPatientId.getIdPart(), "s256", null);

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, codeVerifier,
        "authorization_code", null, null, null, request);
  }

  @Test(expected = OAuth2Exception.class)
  public void testPkceRequiresS256() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId patient/*.r";
    String codeVerifier = UUID.randomUUID().toString() + UUID.randomUUID().toString();
    String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes,
        testPatientId.getIdPart(), "plain", codeVerifier);

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, codeVerifier,
        "authorization_code", null, null, null, request);
  }

  @Test(expected = OAuth2Exception.class)
  public void testPkceRequiredWithV2Scopes() throws Exception {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(TestUtils.TEST_PORT);
    request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

    String scopes = "launch/patient openId patient/*.r";
    String code =
        FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart());

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(code, "SAMPLE_PUBLIC_CLIENT_ID", null, null,
        "authorization_code", null, null, null, request);
  }
  
  @Test
  public void testProcessScopes() {
    List<String> scopesIn = List.of(
        "patient/Patient.read",
        "patient/Observation.rs",
        "patient/*.read",
        "patient/*.*",
        "system/*.*",
        "patient/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item",
        "patient/Condition.rs?category=1&category=2",
        "patient/Condition.rs?category=1&code=2",
        "patient/Condition.rs?badparam=true",
        "openid"
    );
    List<String> scopesOut = AuthorizationController.processScopes(scopesIn);
    
    assertEquals(scopesIn.size() - 1, scopesOut.size()); // only one bad scope gets removed
    int i = 0;
    assertEquals("patient/Patient.rs", scopesOut.get(i++));
    assertEquals("patient/Observation.rs", scopesOut.get(i++));
    assertEquals("patient/*.rs", scopesOut.get(i++));
    assertEquals("patient/*.cruds", scopesOut.get(i++));
    assertEquals("system/*.cruds", scopesOut.get(i++));
    assertEquals("patient/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item", scopesOut.get(i++));
    assertEquals("patient/Condition.rs?category=1&category=2", scopesOut.get(i++));
    assertEquals("patient/Condition.rs?category=1&code=2", scopesOut.get(i++));
    // badparam one is missing
    assertEquals("openid", scopesOut.get(i++));
  }

  /**
   * Common cleanup, run once per class not per test.
   */
  @AfterClass
  public static void afterClass() throws Exception {
    try {
      // delete test patient and encounter

      ourClient.delete().resourceById(testEncounterId)
          .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
              FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
          .execute();

      ourClient.delete().resourceById(testPatientId)
          .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
              FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
          .execute();

      testPatientId = null;
      testEncounterId = null;

      System.setProperty("READ_ONLY", "true");

      // clear db just in case there are any erroneous patients or encounters
      TestUtils.clearDB(ourClient);
    } finally {
      ourServer.stop();
    }
  }

  /**
   * Common setup, run once per class not per test.
   */
  @BeforeClass
  public static void beforeClass() throws Exception {
    Scope.registerSearchParams(Map.of("Condition",
        Set.of("code", "identifier", "patient", "abatement-date", "asserter", "body-site",
            "category", "clinical-status", "encounter", "evidence", "evidence-detail", "onset-date",
            "recorded-date", "severity", "stage", "subject", "verification-status",
            "asserted-date")));
    
    System.setProperty("READ_ONLY", "false");

    testToken = TokenManager.getInstance().getServerToken();

    ourCtx = FhirReferenceServerUtils.FHIR_CONTEXT_R4;

    if (ourPort == 0) {
      ourPort = TestUtils.TEST_PORT;
    }
    ourServer = new Server(ourPort);

    String path = Paths.get("").toAbsolutePath().toString();

    WebAppContext webAppContext = new WebAppContext();
    webAppContext.setContextPath("");
    webAppContext.setDisplayName("HAPI FHIR");
    webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
    webAppContext.setBaseResourceAsString(path + "/src/main/webapp/WEB-INF/");
    webAppContext.setParentLoaderPriority(true);

    ourServer.setHandler(webAppContext);
    ourServer.start();

    ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
    ourServerBase = "http://localhost:" + ourPort + "/reference-server/r4/";

    ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
    ourClient.capabilities();

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 1988);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 13);
    Date birthdate = cal.getTime();
    // ensure that db is not empty (will be deleted @AfterClass)
    Patient pt = new Patient();
    pt.setBirthDate(birthdate);

    pt.addName().setFamily("Test");
    testPatientId = ourClient.create().resource(pt)
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();

    Encounter encounter = new Encounter();
    encounter.setSubject(new Reference().setReference("Patient/" + testPatientId.getIdPart()));
    testEncounterId = ourClient.create().resource(encounter)
        .withAdditionalHeader(AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();
  }

}
