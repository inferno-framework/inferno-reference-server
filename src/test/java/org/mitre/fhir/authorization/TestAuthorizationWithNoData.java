package org.mitre.fhir.authorization;


import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import com.github.dnault.xmlpatch.internal.Log;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Base64;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mitre.fhir.authorization.exception.BearerTokenException;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.TestUtils;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.server.ResponseStatusException;



/**
 * Test cases without preinserting any data for testing things like missing data.
 *
 * @author HERSHIL
 *
 */
public class TestAuthorizationWithNoData {

  private static IGenericClient ourClient;
  private static FhirContext ourCtx;
  private static int ourPort;
  private static Server ourServer;
  private static String ourServerBase;

  @Test(expected = ResponseStatusException.class)
  public void testGetTokenNoEncounterProvided() throws IOException, BearerTokenException {
    Patient pt = new Patient();
    pt.addName().setFamily("Test");

    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(1234);

    String scope = "launch/patient launch/encounter";

    AuthorizationController authorizationController = new AuthorizationController();

    authorizationController.getToken(
        FhirReferenceServerUtils.createCode("SAMPLE_CODE", scope, null),
        "SAMPLE_PUBLIC_CLIENT_ID",
        null,
        null,
        null,
        null,
        null,
        null,
        request
    );

    Token testToken = TokenManager.getInstance().getServerToken();

    IIdType patientId = ourClient.create().resource(pt)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();

    ourClient.delete().resourceById(patientId)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();
  }

  @Test(expected = ResponseStatusException.class)
  public void testGetTokenNoPatientProvided() throws IOException, BearerTokenException {
    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(1234);

    String scope = "launch/patient launch/encounter";

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(FhirReferenceServerUtils.createCode("SAMPLE_CODE", scope, null), "SAMPLE_PUBLIC_CLIENT_ID", null, null, null, null, null, null,
        request);

    Token testToken = TokenManager.getInstance().getServerToken();

    Encounter encounter = new Encounter();

    IIdType encounterId = ourClient.create().resource(encounter)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();

    ourClient.delete().resourceById(encounterId)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();

  }

  @Test(expected = ResponseStatusException.class)
  public void testGetTokenNoPatientOrEncounter() throws IOException, BearerTokenException {

    String serverBaseUrl = "";
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setLocalAddr("localhost");
    request.setRequestURI(serverBaseUrl);
    request.setServerPort(1234);

    String scope = "launch/patient launch/encounter";

    AuthorizationController authorizationController = new AuthorizationController();
    authorizationController.getToken(FhirReferenceServerUtils.createCode("SAMPLE_CODE", scope, null), "SAMPLE_PUBLIC_CLIENT_ID", null, null, null, null, null, null,
        request);
  }

  /**
   * Sets up test server with test data.
   * @throws Exception if server starts incorrectly
   */
  @BeforeClass
  public static void beforeClass() throws Exception {

    ourCtx = FhirContext.forR4();

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
    ourServerBase = "http://localhost:" + ourPort + "/reference-server/r4/";

    ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
    ourClient.registerInterceptor(new LoggingInterceptor(true));
    ourClient.capabilities();

  }

  @AfterClass
  public static void afterClass() throws Exception {
    ourServer.stop();

  }

  @Before
  public void cleanUpBefore() {
    cleanUp();
  }

  @After
  public void cleanUpAfter() {
    cleanUp();
  }

  public void cleanUp() {
    TestUtils.clearDB(ourClient);
  }

}
