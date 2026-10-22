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
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Patient;
import org.junit.AfterClass;
import org.junit.Assert;
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

  @Test(expected = MethodNotAllowedException.class)
  public void testReadOnlyPreventTransaction() throws MethodNotAllowedException {
    Patient pt = new Patient();
    pt.addName().setFamily("Test");
    Bundle bundle = new Bundle();
    bundle.setType(BundleType.TRANSACTION);
    bundle.addEntry().setResource(pt).getRequest().setMethod(HTTPVerb.POST).setUrl("Patient");
    executeBundle(bundle);
  }

  @Test(expected = MethodNotAllowedException.class)
  public void testReadOnlyPreventBatchWithWrite() throws MethodNotAllowedException {
    Bundle bundle = new Bundle();
    bundle.setType(BundleType.BATCH);
    bundle.addEntry().getRequest().setMethod(HTTPVerb.GET).setUrl("Patient");
    bundle.addEntry().getRequest().setMethod(HTTPVerb.DELETE).setUrl("Patient/1234");
    executeBundle(bundle);
  }

  @Test
  public void testReadOnlyAllowReadOnlyBatch() {
    Bundle bundle = new Bundle();
    bundle.setType(BundleType.BATCH);
    bundle.addEntry().getRequest().setMethod(HTTPVerb.GET).setUrl("Patient");
    bundle.addEntry().getRequest().setMethod(HTTPVerb.GET).setUrl("Observation");
    Bundle response = executeBundle(bundle);
    Assert.assertEquals(2, response.getEntry().size());
    for (BundleEntryComponent entry : response.getEntry()) {
      Assert.assertTrue(entry.getResponse().getStatus().startsWith("200"));
    }
  }

  @Test(expected = MethodNotAllowedException.class)
  public void testReadOnlyPreventMetaAdd() throws MethodNotAllowedException {
    Meta meta = new Meta();
    meta.addTag().setSystem("http://example.org").setCode("test");
    String authHeaderValue =
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue());
    ourClient.meta().add().onResource(new IdType("Patient/1234")).meta(meta)
            .withAdditionalHeader(AUTHORIZATION_HEADER_NAME, authHeaderValue)
            .execute();
  }

  @Test(expected = MethodNotAllowedException.class)
  public void testReadOnlyPreventServerOperation() throws MethodNotAllowedException {
    String authHeaderValue =
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue());
    ourClient.operation().onServer().named("$mark-all-resources-for-reindexing")
            .withNoParameters(Parameters.class)
            .withAdditionalHeader(AUTHORIZATION_HEADER_NAME, authHeaderValue)
            .execute();
  }

  @Test
  public void testReadOnlyAllowWhitelistedOperation() {
    String authHeaderValue =
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue());
    Meta meta = ourClient.meta().get(Meta.class).fromType("Patient")
            .withAdditionalHeader(AUTHORIZATION_HEADER_NAME, authHeaderValue)
            .execute();
    Assert.assertNotNull(meta);
  }

  private Bundle executeBundle(Bundle bundle) throws MethodNotAllowedException {
    String authHeaderValue =
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue());
    return ourClient.transaction().withBundle(bundle)
            .withAdditionalHeader(AUTHORIZATION_HEADER_NAME, authHeaderValue)
            .execute();
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
