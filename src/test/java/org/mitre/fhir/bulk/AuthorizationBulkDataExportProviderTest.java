package org.mitre.fhir.bulk;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Group.GroupMemberComponent;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.json.JSONArray;
import org.json.JSONObject;
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

public class AuthorizationBulkDataExportProviderTest {

  private static IGenericClient ourClient;
  private static FhirContext ourCtx;
  private static int ourPort;
  private static Server ourServer;
  private static String ourServerBase;

  private static IIdType groupId;
  private static IIdType testPatientId;
  private static IIdType testEncounterId;
  private static IIdType testOrganizationId;

  private static Token testToken;


  @Test
  public void testGroupBulkExportEndToEnd() throws IOException, InterruptedException {

    // starg
    String urlString = createGroupExport();

    int responseCode = 202;
    HttpURLConnection response = null;

    while (responseCode == 202) {
      response = getCheckExportPollStatusExists(urlString);
      responseCode = response.getResponseCode();
    }

    JSONObject body = getResponseBodyJson(response);

    JSONArray output = (JSONArray) body.get("output");

    Map<String, Integer> numOfResourcesMap = new HashMap<>();

    for (Object resource : output) {
      JSONObject resourceJson = (JSONObject) resource;
      String resourceName = resourceJson.getString("type");

      if (numOfResourcesMap.containsKey(resourceName)) {
        numOfResourcesMap.put(resourceName, numOfResourcesMap.get(resourceName) + 1);
      }

      else {
        numOfResourcesMap.put(resourceName, 1);
      }

    }

    int numOfPatient =
        numOfResourcesMap.get("Patient") != null ? numOfResourcesMap.get("Patient") : 0;
    int numOfEncounters =
        numOfResourcesMap.get("Encounter") != null ? numOfResourcesMap.get("Encounter") : 0;
    int numOfOrganizations =
        numOfResourcesMap.get("Organization") != null ? numOfResourcesMap.get("Organization") : 0;

    Assert.assertEquals(numOfPatient, 1);
    Assert.assertEquals(numOfEncounters, 1);
    Assert.assertEquals(numOfOrganizations, 1);


  }

  private HttpURLConnection getCheckExportPollStatusExists(String urlString) throws IOException {
    URL url = new URL(urlString);
    HttpURLConnection getConnection = (HttpURLConnection) url.openConnection();
    getConnection.setRequestMethod("GET");
    getConnection.setRequestProperty("Accept", "application/json");
    getConnection.setRequestProperty("Authorization", "Bearer " + testToken.getTokenValue());

    getConnection.disconnect();

    return getConnection;
  }

  private JSONObject getResponseBodyJson(HttpURLConnection getConnection) throws IOException {
    InputStream inputStream = getConnection.getInputStream();
    String s = new String(inputStream.readAllBytes());
    JSONObject response;
    if (s != null && !s.equals("")) {
      response = new JSONObject(s);
    }

    else {
      response = new JSONObject();
    }
    return response;
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

    Encounter encounter = new Encounter();
    encounter.setSubject(new Reference().setReference("Patient/" + testPatientId.getIdPart()));
    testEncounterId = ourClient.create().resource(encounter)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();

    Organization organization = new Organization();
    testOrganizationId = ourClient.create().resource(organization)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute().getId();

    Group group = new Group();
    group.setName("Test Name");

    Reference patientReference = new Reference();
    patientReference.setResource(pt);
    patientReference.setReference("Patient/" + testPatientId.getIdPart());
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


    ourClient.delete().resourceById(testEncounterId)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();

    ourClient.delete().resourceById(testPatientId)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();

    ourClient.delete().resourceById(testOrganizationId)
        .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
            FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
        .execute();

    // clear db just in case there are any erroneous patients or encounters
    TestUtils.clearDB(ourClient);

    ourServer.stop();
  }

}
