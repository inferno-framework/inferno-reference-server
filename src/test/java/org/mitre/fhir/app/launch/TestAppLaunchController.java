package org.mitre.fhir.app.launch;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonString;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.TestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Paths;
import java.util.*;

public class TestAppLaunchController {

  private static IGenericClient ourClient;
  private static int ourPort;
  private static Server ourServer;
  private static IIdType testFirstPatientId;
  private static IIdType testFirstEncounterId;
  private static Token testToken;

  @Test
   public void testGetEhrLaunchContextOptions() throws IOException {
    AppLaunchController appLaunchController = new AppLaunchController();
    MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    mockHttpServletRequest.setServerName("localhost:" + ourServer.getURI().getPort());
    mockHttpServletRequest.setRequestURI("/app/ehr-launch-context-options");

    ResponseEntity<String> response = appLaunchController.getEhrLaunchContextOptions(mockHttpServletRequest);

    String patientId = testFirstPatientId.getIdPart();
    JsonObject jsonResponseBody = JSON.parse(Objects.requireNonNull(response.getBody()));
    String returnedEncounterId = jsonResponseBody.get(patientId).getAsArray().get(0).toString();
    String encounterId = new JsonString(testFirstEncounterId.getIdPart()).toString();
    assert(Objects.equals(encounterId, returnedEncounterId));
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    System.setProperty("READ_ONLY", "false");

    testToken = TokenManager.getInstance().getServerToken();
    FhirContext ourCtx = FhirContext.forR4();

    if (ourPort == 0) { ourPort = TestUtils.TEST_PORT; };
    ourServer = new Server(ourPort);

    String path = Paths.get("").toAbsolutePath().toString();

    WebAppContext webAppContext = new WebAppContext();
    webAppContext.setDisplayName("HAPI FHIR");
    webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
    webAppContext.setResourceBase(path + "/target/mitre-fhir-starter");
    webAppContext.setParentLoaderPriority(true);

    ourServer.setHandler(webAppContext);
    ourServer.start();

    ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
    ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
    String ourServerBase = "http://localhost:" + ourPort + "/reference-server/r4/";

    ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
    ourClient.capabilities();

    Calendar cal = Calendar.getInstance();
    cal.set(Calendar.YEAR, 1988);
    cal.set(Calendar.MONTH, Calendar.JANUARY);
    cal.set(Calendar.DAY_OF_MONTH, 13);
    Date birthdate = cal.getTime();
    // ensure that db is not empty (will be deleted @AfterClass)
    Patient pt1 = new Patient();
    pt1.setBirthDate(birthdate);

    pt1.addName().setFamily("Test1");
    testFirstPatientId = ourClient.create().resource(pt1)
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute().getId();

    Encounter firstEncounter = new Encounter();
    firstEncounter.setSubject(new Reference().setReference("Patient/" + testFirstPatientId.getIdPart()));
    testFirstEncounterId = ourClient.create().resource(firstEncounter)
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute().getId();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    // delete test patient and encounter
    ourClient.delete().resourceById("Encounter", testFirstEncounterId.getIdPart())
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute();

    ourClient.delete().resourceById("Patient", testFirstPatientId.getIdPart())
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute();

    System.setProperty("READ_ONLY", "true");

    testFirstPatientId = null;
    testFirstEncounterId = null;

    // clear db just in case there are any erroneous patients or encounters
    TestUtils.clearDB(ourClient);

    ourServer.stop();
  }
}
