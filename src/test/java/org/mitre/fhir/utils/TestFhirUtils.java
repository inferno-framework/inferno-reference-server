package org.mitre.fhir.utils;

import static org.junit.Assert.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;

public class TestFhirUtils {

  private static IGenericClient ourClient;
  private static int ourPort;
  private static Server ourServer;
  private static IIdType testFirstPatientId;
  private static IIdType testSecondPatientId;
  private static IIdType testFirstEncounterId;
  private static IIdType testSecondEncounterId;
  private static IIdType testThirdEncounterId;
  private static Token testToken;

  @Test
  public void testGetAllEncountersWithPatientId() {
    List<Bundle.BundleEntryComponent> bundle =
        FhirUtils.getAllEncountersWithPatientId(ourClient, testFirstPatientId.getIdPart());
    assertTrue(bundle.size() == 2);
    Resource firstEntry = bundle.get(0).getResource();
    Resource secondEntry = bundle.get(1).getResource();
    assertTrue(firstEntry.getClass() == Encounter.class);
    assertTrue(secondEntry.getClass() == Encounter.class);
    assertTrue(Objects.equals(firstEntry.getId(), testFirstEncounterId.toString()));
    assertTrue(Objects.equals(secondEntry.getId(), testSecondEncounterId.toString()));

    bundle = FhirUtils.getAllEncountersWithPatientId(ourClient, testSecondPatientId.getIdPart());
    assertTrue(bundle.size() == 1);
    firstEntry = bundle.get(0).getResource();
    assertTrue(firstEntry.getClass() == Encounter.class);
    assertTrue(Objects.equals(firstEntry.getId(), testThirdEncounterId.toString()));
  }

  /**
   * Common setup, run once per class not per test.
   */
  @BeforeClass
  public static void beforeClass() throws Exception {
    System.setProperty("READ_ONLY", "false");

    testToken = TokenManager.getInstance().getServerToken();

    if (ourPort == 0) {
      ourPort = TestUtils.TEST_PORT;
    }
    ourServer = new Server(ourPort);

    String path = Paths.get("").toAbsolutePath().toString();

    WebAppContext webAppContext = new WebAppContext();
    webAppContext.setDisplayName("HAPI FHIR");
    webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
    webAppContext.setBaseResourceAsString(path + "/src/main/webapp/WEB-INF/");
    webAppContext.setParentLoaderPriority(true);

    ourServer.setHandler(webAppContext);
    ourServer.start();

    FhirContext ourCtx = FhirReferenceServerUtils.FHIR_CONTEXT_R4;
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

    Patient pt2 = new Patient();
    pt2.setBirthDate(birthdate);

    pt1.addName().setFamily("Test1");
    testFirstPatientId = ourClient.create().resource(pt1)
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute().getId();

    pt2.addName().setFamily("Test2");
    testSecondPatientId = ourClient.create().resource(pt2)
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute().getId();
    
    Encounter firstEncounter = new Encounter();
    firstEncounter.setSubject(new Reference("Patient/" + testFirstPatientId.getIdPart()));
    testFirstEncounterId = ourClient.create().resource(firstEncounter)
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute().getId();

    Encounter secondEncounter = new Encounter();
    secondEncounter.setSubject(new Reference("Patient/" + testFirstPatientId.getIdPart()));
    testSecondEncounterId = ourClient.create().resource(secondEncounter)
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute().getId();

    Encounter thirdEncounter = new Encounter();
    thirdEncounter.setSubject(new Reference("Patient/" + testSecondPatientId.getIdPart()));
    testThirdEncounterId = ourClient.create().resource(thirdEncounter)
     .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
      FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
     .execute().getId();
  }

  /**
   * Common cleanup, run once per class not per test.
   */
  @AfterClass
  public static void afterClass() throws Exception {
    // delete test patient and encounter
    try {
      ourClient.delete().resourceById("Encounter", testFirstEncounterId.getIdPart())
       .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
       .execute();

      ourClient.delete().resourceById("Encounter", testSecondEncounterId.getIdPart())
       .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
       .execute();

      ourClient.delete().resourceById("Encounter", testThirdEncounterId.getIdPart())
       .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
       .execute();

      ourClient.delete().resourceById("Patient", testFirstPatientId.getIdPart())
       .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
       .execute();

      ourClient.delete().resourceById("Patient", testSecondPatientId.getIdPart())
       .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
        FhirReferenceServerUtils.createAuthorizationHeaderValue(testToken.getTokenValue()))
       .execute();

      System.setProperty("READ_ONLY", "true");

      testFirstPatientId = null;
      testSecondPatientId = null;
      testFirstEncounterId = null;
      testSecondEncounterId = null;
      testThirdEncounterId = null;

      // clear db just in case there are any erroneous patients or encounters
      TestUtils.clearDB(ourClient);
    } finally {
      ourServer.stop();
    }
  }
}
