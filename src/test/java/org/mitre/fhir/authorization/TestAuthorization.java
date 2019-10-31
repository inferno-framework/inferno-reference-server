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
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CapabilityStatement;
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
import com.github.dnault.xmlpatch.internal.Log;

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

	static {
		ourCtx = FhirContext.forR4();
	}

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

		// delete the new entry so the db
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
			request.setServerPort(1234);

			authorizationController.getToken("INVALID_CODE", null, null, request);
			// did not get expected exception
			Assert.fail("Did not get expected Unauthorized ResponseStatusException");
		}

		catch (ResponseStatusException rse) {
			if (!HttpStatus.UNAUTHORIZED.equals(rse.getStatus())) {
				// did not get expected exception with correct response code
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
		request.setServerPort(1234);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

		ResponseEntity<String> tokenResponseEntity = authorizationController
				.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);

		ObjectMapper mapper = new ObjectMapper();

		String jSONString = tokenResponseEntity.getBody();

		JsonNode jsonNode = mapper.readTree(jSONString);
		String accessToken = jsonNode.get("access_token").asText();

		Assert.assertEquals("SAMPLE_ACCESS_TOKEN", accessToken);
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
		request.setServerPort(1234);

		// shouldn't throw an exception
		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE,
				FhirReferenceServerUtils.PUBLIC_CLIENT_ID, null, request);
	}

	@Test(expected = InvalidClientIdException.class)
	public void testGetTokenWithoutBasicAuthAndInvalidClientId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(1234);

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, "INVALID_CLIENT_ID", null, request);
	}

	@Test(expected = InvalidClientIdException.class)
	public void testGetTokenWithoutBasicAuthAndNullClientId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(1234);

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);
	}

	@Test
	public void testGetTokenWithBasicAuth() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(1234);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);
	}

	@Test(expected = InvalidClientIdException.class)
	public void testGetTokenWithBasicAuthWithInvalidClientId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(1234);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader("INVALID_CLIENT_ID",
				FhirReferenceServerUtils.SAMPLE_CLIENT_SECRET));

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);
	}

	@Test
	public void testGetTokenWithBasicAuthWithConfidentialClientId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(1234);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader(
				FhirReferenceServerUtils.CONFIDENTIAL_CLIENT_ID, FhirReferenceServerUtils.SAMPLE_CLIENT_SECRET));

		// no error should be thrown
		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);
	}

	@Test(expected = InvalidClientSecretException.class)
	public void testGetTokenWithBasicAuthWithInvalidClientSecret() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(1234);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader(
				FhirReferenceServerUtils.CONFIDENTIAL_CLIENT_ID, "Invalid Client Secret"));

		authorizationController.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);
	}

	@Test
	public void testGetTokenGivesValidOpenId() {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(1234);
		request.addHeader("Authorization", TestUtils.getEncodedBasicAuthorizationHeader());

		ResponseEntity<String> tokenResponseEntity = authorizationController
				.getToken(FhirReferenceServerUtils.SAMPLE_CODE, null, null, request);
		String jSONString = tokenResponseEntity.getBody();
		JSONObject jSONObject = new JSONObject(jSONString);
		String idToken = (String) jSONObject.get("id_token");

		// will throw an exception if invalid
		DecodedJWT decoded = JWT.decode(idToken);
		Algorithm algorithm = Algorithm.RSA256(RSAUtils.getRSAPublicKey(), null);

		// verify signature
		JWT.require(algorithm).build().verify(decoded);

		// test some of the fields of decoded jwt
		Assert.assertEquals("RS256", decoded.getAlgorithm());
		Assert.assertNotNull(decoded.getClaim("fhirUser"));

	}

	@AfterClass
	public static void afterClass() throws Exception {
		// delete test Patient that was added in @Before class
		ourClient.delete().resourceById(testPatientId)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE)
				.execute();
		ourServer.stop();
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		String path = Paths.get("").toAbsolutePath().toString();

		Log.info("Project base path is: " + path + " is our port " + ourPort);

		if (ourPort == 0) {
			ourPort = 1234;
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

	}

}
