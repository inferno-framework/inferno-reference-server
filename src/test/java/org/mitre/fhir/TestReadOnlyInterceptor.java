package org.mitre.fhir;

import static org.mitre.fhir.utils.FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import java.nio.file.Paths;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.hl7.fhir.r4.model.Patient;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.TestUtils;

public class TestReadOnlyInterceptor {

  private static int ourPort;
  private static Token testToken;
  private static Server ourServer;
  private static FhirContext ourCtx;
  private static String ourServerBase;
  private static IGenericClient ourClient;

  @Test(expected = MethodNotAllowedException.class)
  public void testReadOnlyPreventCreate() throws MethodNotAllowedException {
    createPatient();
  }

  @Test(expected = MethodNotAllowedException.class)
  public void testReadOnlyPreventUpdate() throws MethodNotAllowedException {
    updatePatient();
  }

  @Test(expected = MethodNotAllowedException.class)
  public void testReadOnlyPreventDelete() throws MethodNotAllowedException {
    deletePatient();
  }

  private void createPatient() throws MethodNotAllowedException {
    Patient pt = new Patient();
    pt.addName().setFamily("Test");
    String authHeaderValue =
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue());
    ourClient.create().resource(pt)
            .withAdditionalHeader(AUTHORIZATION_HEADER_NAME, authHeaderValue)
            .execute();
  }

  private void updatePatient() throws MethodNotAllowedException {
    Patient pt = new Patient();
    pt.addName().setFamily("Test");
    pt.setId("1234");
    String authHeaderValue =
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue());
    ourClient.update().resource(pt)
            .withAdditionalHeader(AUTHORIZATION_HEADER_NAME, authHeaderValue)
            .execute();
  }

  private void deletePatient() throws MethodNotAllowedException {
    Patient pt = new Patient();
    pt.addName().setFamily("Test");
    pt.setId("Patient/1234");
    String authHeaderValue =
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue());
    ourClient.delete().resource(pt)
            .withAdditionalHeader(AUTHORIZATION_HEADER_NAME, authHeaderValue)
            .execute();
  }

  /**
   * Common setup, run once per class not per test.
   */
  @BeforeClass
  public static void beforeClass() throws Exception {

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
    ourClient.registerInterceptor(new LoggingInterceptor(true));
    ourClient.capabilities();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    ourServer.stop();
  }
}
