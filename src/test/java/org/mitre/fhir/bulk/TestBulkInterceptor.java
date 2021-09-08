package org.mitre.fhir.bulk;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.TestUtils;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;

public class TestBulkInterceptor {

  private static IGenericClient ourClient;
  private static FhirContext ourCtx;
  private static int ourPort;
  private static Server ourServer;
  private static String ourServerBase;

  private static IIdType groupId;
  private static IIdType testPatientId;

  private static Token testToken;

  @Test
  public void testBulkInterceptor() throws IOException {
    String urlString = createGroupExport();
    Assert.assertTrue(checkExportPollStatusExists(urlString));
    //confirm that delete works and is being routed to the proper delete method
    deleteGroupExport(urlString);
    Assert.assertFalse(checkExportPollStatusExists(urlString));
  }
  
  private void deleteGroupExport(String urlString) throws IOException
  {
    URL url = new URL(urlString);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("DELETE");
    conn.setRequestProperty("Accept", "application/json");
    conn.setRequestProperty("Authorization", "Bearer " + testToken.getTokenValue());
    
    //Calls the service
    conn.getResponseCode();

    conn.disconnect();

  }

  private boolean checkExportPollStatusExists(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection getConnection = (HttpURLConnection) url.openConnection();
    getConnection.setRequestMethod("GET");
    getConnection.setRequestProperty("Accept", "application/json");
    getConnection.setRequestProperty("Authorization", "Bearer " + testToken.getTokenValue());
    int responseCode = getConnection.getResponseCode();
    getConnection.disconnect();
    return responseCode != 404;
  }

  private String createGroupExport() throws IOException {

    URL url = new URL(ourServerBase + "Group/" + groupId.getIdPart() + "/$export");

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    conn.setRequestProperty("Accept", "application/fhir+json");
    conn.setRequestProperty("prefer", "respond-async");
    conn.setRequestProperty("Authorization", "Bearer " + testToken.getTokenValue());

    String contentUrl = conn.getHeaderField("Content-Location");

    conn.disconnect();

    return contentUrl;
  }


  @BeforeClass
  public static void beforeClass() throws Exception {


    testToken = TokenManager.getInstance().getServerToken();

    ourCtx = FhirContext.forR4();

    if (ourPort == 0) {
      ourPort = TestUtils.TEST_PORT;
    }
    ourServer = new Server(ourPort);

    String path = Paths.get("").toAbsolutePath().toString();

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

    // ensure that db is not empty (will be deleted @AfterClass)
    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 1988);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 13);
    Date birthdate = cal.getTime();
    Patient pt = new Patient();
    pt.setBirthDate(birthdate);

    pt.addName().setFamily("Test");
    testPatientId = ourClient.create().resource(pt)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();

    Group group = new Group();
    group.setName("Test Name");
    Reference patientReference = new Reference();
    patientReference.setResource(pt);
    GroupMemberComponent patientMember = new GroupMemberComponent(patientReference);
    group.addMember(patientMember);

    groupId = ourClient.create().resource(group)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();

  }

  @AfterClass
  public static void afterClass() throws Exception {

    // delete test patient and group
    ourClient.delete().resourceById(groupId)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();

    ourClient.delete().resourceById(testPatientId)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();

    // clear db just in case there are any erroneous patients or encounters
    TestUtils.clearDB(ourClient);

    ourServer.stop();
  }

}
