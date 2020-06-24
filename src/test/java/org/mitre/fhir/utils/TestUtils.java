
package org.mitre.fhir.utils;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.mitre.fhir.authorization.token.Token;
import org.mitre.fhir.authorization.token.TokenManager;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.FhirUtils;

import java.util.Base64;
import java.util.Base64.Encoder;
import java.util.List;

public class TestUtils {

  public static final String BEARER_TOKEN_PREFIX = "Bearer";

  public static final int TEST_PORT = 1234;

  public static String getBasicAuthorizationString(String clientId, String clientSecret) {
    return clientId + ":" + clientSecret;
  }

  public static String getEncodedBasicAuthorizationHeader() {
    return getEncodedBasicAuthorizationHeader(
        FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_ID,
        FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_SECRET);
  }

  public static String getEncodedBasicAuthorizationHeader(String clientId, String clientSecret) {
    Encoder encoder = Base64.getUrlEncoder();
    String decodedValue = getBasicAuthorizationString(clientId, clientSecret);
    String encodedValue = encoder.encodeToString(decodedValue.getBytes());
    return "Basic " + encodedValue;
  }

  public static void clearDB(IGenericClient ourClient) {

    // confirm that this is only being called on test data
    if (!ourClient.getServerBase().startsWith("http://localhost:" + TEST_PORT)) {
      throw new RuntimeException("ClearDB should ONLY be used on tests!");
    }

    clearAllPatientsFromDB(ourClient);
    clearAllEncountersFromDB(ourClient);
  }

  private static void clearAllPatientsFromDB(IGenericClient ourClient) {
    Token token = TokenManager.getInstance().getServerToken();

    List<BundleEntryComponent> patients = FhirUtils.getAllPatients(ourClient);

    for (BundleEntryComponent bundleEntryComponent : patients) {
      Patient patient = (Patient) bundleEntryComponent.getResource();
      System.out.println("Deleting Patient " + patient.getIdElement().getIdPart());
      ourClient.delete().resource(patient)
          .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
              TestUtils.getAuthorizationHeaderBearerValue(token.getTokenValue()))
          .execute();
    }

  }

  private static void clearAllEncountersFromDB(IGenericClient ourClient) {
    Token token = TokenManager.getInstance().getServerToken();

    List<BundleEntryComponent> encounters = FhirUtils.getAllEncounters(ourClient);

    for (BundleEntryComponent bundleEntryComponent : encounters) {
      Encounter encounter = (Encounter) bundleEntryComponent.getResource();
      System.out.println("Deleting Encounter " + encounter.getIdElement().getIdPart());
      ourClient.delete().resource(encounter)
          .withAdditionalHeader(FhirReferenceServerUtils.AUTHORIZATION_HEADER_NAME,
              TestUtils.getAuthorizationHeaderBearerValue(token.getTokenValue()))
          .execute();
    }
  }

  public static String getAuthorizationHeaderBearerValue(String accessToken) {
    return BEARER_TOKEN_PREFIX + " " + accessToken;
  }

}
