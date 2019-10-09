package org.mitre.fhir.authorization;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;

import org.mitre.fhir.authorization.TestUtils;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Patient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;

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
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE)
				.execute().getId();

		Patient pt2 = ourClient.read().resource(Patient.class).withId(id)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE)
				.execute();
		assertEquals(methodName, pt2.getName().get(0).getFamily());

		// delete the new entry so the db
		ourClient.delete().resourceById(id)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE)
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
			String serverBaseUrl = "/mitre-fhir";
			MockHttpServletRequest request = new MockHttpServletRequest();
			request.setLocalAddr("localhost");
			request.setRequestURI(serverBaseUrl);
			request.setServerPort(1234);

			authorizationController.getToken("INVALID_CODE", null, "SAMPLE_CLIENT_ID", request);
			//did not get expected exception
			Assert.fail("Did not get expected Unauthorized ResponseStatusException");
		}

		catch (ResponseStatusException rse) {
			if (!HttpStatus.UNAUTHORIZED.equals(rse.getStatus())) {
				//did not get expected exception with correct response code
				throw rse;
			}
		}
	}

	@Test
	public void testTestAuthorizationWithValidCode() throws IOException {
		AuthorizationController authorizationController = new AuthorizationController();
		String serverBaseUrl = "/mitre-fhir";
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setLocalAddr("localhost");
		request.setRequestURI(serverBaseUrl);
		request.setServerPort(1234);

		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken("SAMPLE_CODE", null,
				"SAMPLE_CLIENT_ID", request);

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

	@AfterClass
	public static void afterClass() throws Exception {
		//delete test Patient that was added in @Before class
		ourClient.delete().resourceById(testPatientId).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE).execute();
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
		webAppContext.setContextPath("/mitre-fhir");
		webAppContext.setDisplayName("HAPI FHIR");
		webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
		webAppContext.setResourceBase(path + "/target/mitre-fhir-starter");
		webAppContext.setParentLoaderPriority(true);

		ourServer.setHandler(webAppContext);
		ourServer.start();

		ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
		ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
		ourServerBase = "http://localhost:" + ourPort + "/mitre-fhir/r4/";

		ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
		ourClient.registerInterceptor(new LoggingInterceptor(true));
		ourClient.capabilities();
		
		//ensure that db is not empty (will be deleted @AfterClass)
		Patient pt = new Patient();
		pt.addName().setFamily("Test");
		testPatientId = ourClient.create().resource(pt)
				.withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE)
				.execute().getId();

		
		
	}

}
