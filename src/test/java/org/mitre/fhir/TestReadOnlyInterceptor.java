package org.mitre.fhir;

import org.junit.Test;
import java.nio.file.Paths;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.hl7.fhir.r4.model.Patient;
import org.mitre.fhir.utils.TestUtils;
import org.eclipse.jetty.server.Server;
import ca.uhn.fhir.context.FhirContext;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.mitre.fhir.authorization.token.Token;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.authorization.token.TokenManager;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;

public class TestReadOnlyInterceptor {

  private static int ourPort;
  private static Token testToken;
  private static Server ourServer;
  private static FhirContext ourCtx;
  private static String ourServerBase;
  private static IGenericClient ourClient;
  private static String ConfigFileContent;

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
    ourClient.create().resource(pt)
            .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
                    FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
            .execute();
  }

  private void updatePatient() throws MethodNotAllowedException {
    Patient pt = new Patient();
    pt.addName().setFamily("Test");
    pt.setId("1234");
    ourClient.update().resource(pt)
            .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
                    FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
            .execute();
  }

  private void deletePatient() throws MethodNotAllowedException {
    Patient pt = new Patient();
    pt.addName().setFamily("Test");
    pt.setId("Patient/1234");
    ourClient.delete().resource(pt)
            .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
                    FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
            .execute();
  }

  @BeforeClass
  public static void beforeClass() throws Exception {

    testToken = TokenManager.getInstance().getServerToken();
    ourCtx = FhirContext.forR4();

    if (ourPort == 0) { ourPort = TestUtils.TEST_PORT; }

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
