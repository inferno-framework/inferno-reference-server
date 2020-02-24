package org.mitre.fhir.authorization;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

import org.mitre.fhir.authorization.TestUtils;
import org.mitre.fhir.authorization.exception.InvalidClientIdException;
import org.mitre.fhir.authorization.exception.InvalidClientSecretException;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.RSAUtils;
import org.mitre.fhir.utils.exception.RSAKeyException;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.assertEquals;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;
import java.nio.file.Paths;

public class TestAuthorization {

	private static IGenericClient ourClient;
	private static FhirContext ourCtx;
	private static int ourPort;
	private static Server ourServer;
	private static String ourServerBase;

	private static IIdType testPatientId;
	private static IIdType testEncounterId;
	
	private static final String SAMPLE_CODE = "SAMPLE_CODE"; 

	@Test
	public void testCreateAndRead() {

		String methodName = "testCreateResourceConditional";

		Patient pt = new Patient();
		pt.addName().setFamily(methodName);
		IIdType id = ourClient.create().resource(pt)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE)
				.execute().getId();
		
		Patient pt2 = ourClient.read().resource(Patient.class).withId(id)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE)
				.execute();
		assertEquals(methodName, pt2.getName().get(0).getFamily());

		// delete the new entry so the db won't have a leftover artifact
		ourClient.delete().resourceById(id)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE)
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

	@Test
	public void testTestAuthorizationWithInvalidCode() {
		AuthorizationController authorizationController = new AuthorizationController();

		try {
			String serverBaseUrl = "";
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setLocalAddr("localhost");
			request.setRequestURI(serverBaseUrl);
			request.setServerPort(TestUtils.TEST_PORT);

			authorizationController.getToken("INVALID_CODE", null, null, request);
			Assert.fail("Did not get expected Unauthorized ResponseStatusException");
		}

		catch (ResponseStatusException rse) {
			if (!HttpStatus.UNAUTHORIZED.equals(rse.getStatus())) {
				throw rse;
			}
		}
	}

	@Test
	public void testTestAuthorizationWithNullCode() {
		AuthorizationController authorizationController = new AuthorizationController();

		try {
			String serverBaseUrl = "";
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setLocalAddr("localhost");
			request.setRequestURI(serverBaseUrl);
			request.setServerPort(TestUtils.TEST_PORT);

			authorizationController.getToken(null, null, "SAMPLE_CLIENT_ID", request);
			Assert.fail("Did not get expected Unauthorized ResponseStatusException");
		}

		catch (ResponseStatusException rse) {
			if (!HttpStatus.UNAUTHORIZED.equals(rse.getStatus())) {
				throw rse;
			}
		}
	}

	@Test
	public void testTestAuthorizationWithValidCode() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

		String scopes = "launch/patient openId ";
		String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart());
		
		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(code,
				"SAMPLE_PUBLIC_CLIENT_ID", null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		String accessToken = jsonNode.get("access_token").asText();

		Assert.assertEquals("SAMPLE_ACCESS_TOKEN", accessToken);
	}
		
	@Test
	public void testReadScopeNoScopeProvided() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		String scopes = "";
		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
				"SAMPLE_PUBLIC_CLIENT_ID", null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		String scope = jsonNode.get("scope").asText();

		Assert.assertEquals("", scope);
	}

	@Test
	public void testReadScope() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		String scopes = "/patient openId _-\\/";

		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
				"SAMPLE_PUBLIC_CLIENT_ID", null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		String scopeResult = jsonNode.get("scope").asText();

		Assert.assertEquals(scopes, scopeResult);
	}

	@Test
	public void testCapabilityStatementNotBlockedByInterceptor() {
		// should throw an exception if intercepter does not white list it
		ourClient.capabilities().ofType(CapabilityStatement.class).execute();
	}

	@Test
	public void testGetTokenWithoutBasicAuth() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		String scopes = "";
		// shouldn't throw an exception
		authorizationController.getToken(FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
				FhirReferenceServerUtils.SAMPLE_PUBLIC_CLIENT_ID, null, request);
	}

	@Test(expected = InvalidClientIdException.class)
	public void testGetTokenWithoutBasicAuthAndInvalidClientId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, "INVALID_CLIENT_ID", null, request);
	}

	@Test(expected = InvalidClientIdException.class)
	public void testGetTokenWithoutBasicAuthAndNullClientId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);
	}

	@Test
	public void testGetTokenWithBasicAuth() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

		authorizationController.getToken(FhirReferenceServerUtils.createCode(SAMPLE_CODE, "", testPatientId.getIdPart()), null, null, request);
	}

	@Test(expected = ResponseStatusException.class)
	public void testGetTokenNoPatientScopeProvided() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);

	}

	@Test
	public void testGetTokenNoEncounterScopeProvided() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		String scopes = "launch/patient openId ";
		
		String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart());

		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(code,
				"SAMPLE_PUBLIC_CLIENT_ID", null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		JsonNode patientId = jsonNode.get("patient");
		JsonNode encounterId = jsonNode.get("encounter");

		Assert.assertNotNull(patientId);
		Assert.assertNull(encounterId);
	}

	@Test
	public void testGetTokenNoPatientOrEncounterScopeProvided() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		String scopes = "";

		String code = FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart());
		
		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(code,
				"SAMPLE_PUBLIC_CLIENT_ID", null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		JsonNode patientId = jsonNode.get("patient");
		JsonNode encounterId = jsonNode.get("encounter");

		Assert.assertNull(patientId);
		Assert.assertNull(encounterId);
	}

	@Test
	public void testPatientAndEncounterScopeProvided() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		String scopes = "launch/patient launch/encounter ";

		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
				"SAMPLE_PUBLIC_CLIENT_ID", null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		String patientId = jsonNode.get("patient").asText();
		String encounterId = jsonNode.get("encounter").asText();

		Assert.assertNotNull(patientId);
		Assert.assertNotNull(encounterId);
	}

	@Test
	public void testNoPatientScopeButEncounterScopeProvided() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);

		String scopes = "launch/encounter ";

		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()),
				"SAMPLE_PUBLIC_CLIENT_ID", null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		JsonNode patientId = jsonNode.get("patient");
		JsonNode encounterId = jsonNode.get("encounter");

		Assert.assertNull(patientId);
		Assert.assertNotNull(encounterId);
	}

	@Test(expected = InvalidClientIdException.class)
	public void testGetTokenWithBasicAuthWithInvalidClientId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader("INVALID_CLIENT_ID",
				FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_SECRET));
		String encodedScopes = "";
		String code = FhirReferenceServerUtils.SAMPLE_CODE + encodedScopes;
		authorizationController.getToken(code, null, null, request);
	}

	@Test
	public void testGetTokenWithBasicAuthWithConfidentialClientId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization",
				TestUtils.getEncodedBasicAuthorizationHeader(FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_ID,
						FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_SECRET));
		String scopes = "";
		
		// no error should be thrown
		authorizationController.getToken(FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()), null, null, request);
	}

	@Test(expected = InvalidClientSecretException.class)
	public void testGetTokenWithBasicAuthWithInvalidClientSecret() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader(
				FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_ID, "Invalid Client Secret"));

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);
	}

	@Test
	public void testGetTokenGivesValidOpenId() throws IllegalArgumentException, RSAKeyException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

		String scopes = "";
		ResponseEntity<String> tokenResponseEntity = authorizationController
				.getToken(FhirReferenceServerUtils.createCode(SAMPLE_CODE, scopes, testPatientId.getIdPart()), null, null, request);
		String jSONString = tokenResponseEntity.getBody();
		JSONObject jSONObject = new JSONObject(jSONString);
		String idToken = (String) jSONObject.get("id_token");

		// will throw an exception if invalid
		DecodedJWT decoded = JWT.decode(idToken);
		Algorithm algorithm = Algorithm.RSA256(RSAUtils.getRSAPublicKey(), null);

		JWT.require(algorithm).build().verify(decoded);

		Assert.assertEquals("RS256", decoded.getAlgorithm());
		Assert.assertNotNull(decoded.getClaim("fhirUser"));
	}
	
	@Test
	public void testTestAuthorizationWithRefreshToken() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

		String scopes = "launch/patient openId ";
		String refreshToken = FhirReferenceServerUtils.createCode(FhirReferenceServerUtils.SAMPLE_REFRESH_TOKEN, scopes, testPatientId.getIdPart());
		
		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(null,
				"SAMPLE_PUBLIC_CLIENT_ID", refreshToken, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		String accessToken = jsonNode.get("access_token").asText();

		Assert.assertEquals("SAMPLE_ACCESS_TOKEN", accessToken);
	}
	
	@Test(expected = ResponseStatusException.class)
	public void testTestAuthorizationNoCodeAndNoRefreshToken() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());
		
		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(null,
				"SAMPLE_PUBLIC_CLIENT_ID", null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		String accessToken = jsonNode.get("access_token").asText();

		Assert.assertEquals("SAMPLE_ACCESS_TOKEN", accessToken);
	}
	
	@Test(expected = ResponseStatusException.class)
	public void testTestAuthorizationInvalidRefreshToken() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(TestUtils.TEST_PORT);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());
		
		String scopes = "launch/patient openId ";
		String refreshToken = FhirReferenceServerUtils.createCode("INCORRECT_REFRESH_TOKEN", scopes, testPatientId.getIdPart());
	
		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken(null,
				"SAMPLE_PUBLIC_CLIENT_ID", refreshToken, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		String accessToken = jsonNode.get("access_token").asText();

		Assert.assertEquals("SAMPLE_ACCESS_TOKEN", accessToken);
	}

	@AfterClass
	public static void afterClass() throws Exception {

		// delete test patient and encounter
		ourClient.delete().resourceById(testPatientId)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE)
				.execute();

		ourClient.delete().resourceById(testEncounterId)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE)
				.execute();

		testPatientId = null;
		testEncounterId = null;

		// clear db just in case there are any erroneous patients or encounters
		TestUtils.clearDB(ourClient);

		ourServer.stop();
	}

	@BeforeClass
	public static void beforeClass() throws Exception {

		String path = Paths.get("").toAbsolutePath().toString();

		ourCtx = FhirContext.forR4();

		if (ourPort == 0) {
			ourPort = TestUtils.TEST_PORT;
		}
		ourServer = new Server(ourPort);

		WebAppContext webAppContext = new WebAppContext();
		webAppContext.setContextPath("");
		webAppContext.setDisplayName("HAPI FHIR");
		webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
		webAppContext.setResourceBase(path + "/target/mitre-fhir-starter");
		webAppContext.setParentLoaderPriority(true);

		ourServer.setHandler(webAppContext);
		ourServer.start();

		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		ourServerBase = "http://localhost:" + ourPort + "/r4/";

		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
		ourClient.registerInterceptor(new LoggingInterceptor(true));
		ourClient.capabilities();

		// ensure that db is not empty (will be deleted @AfterClass)
		Patient pt = new Patient();
		pt.addName().setFamily("Test");
		testPatientId = ourClient.create().resource(pt)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE)
				.execute().getId();

		Encounter encounter = new Encounter();
		testEncounterId = ourClient.create().resource(encounter)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE)
				.execute().getId();

	}

}
