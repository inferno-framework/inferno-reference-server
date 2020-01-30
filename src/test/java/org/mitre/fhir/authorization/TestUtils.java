package org.mitre.fhir.authorization;

import java.util.Base64;
import java.util.List;
import java.util.Base64.Encoder;

import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.FhirUtils;

import ca.uhn.fhir.rest.client.api.IGenericClient;

public class TestUtils {

	public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
	public static final String AUTHORIZATION_HEADER_BEARER_VALUE = "Bearer SAMPLE_ACCESS_TOKEN";

	public static final int TEST_PORT = 1234;
	
	public static String getBasicAuthorizationString(String clientId, String clientSecret) {
		return clientId + ":" + clientSecret;
	}

	public static String getEncodedBasicAuthorizationHeader() {
		return getEncodedBasicAuthorizationHeader(FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_ID,
				FhirReferenceServerUtils.SAMPLE_CONFIDENTIAL_CLIENT_SECRET);
	}

	public static String getEncodedBasicAuthorizationHeader(String clientId, String clientSecret) {
		Encoder encoder = Base64.getUrlEncoder();
		String decodedValue = getBasicAuthorizationString(clientId, clientSecret);
		String encodedValue = encoder.encodeToString(decodedValue.getBytes());
		return "Basic " + encodedValue;
	}
	
	public static String createCode(String actualCode, String scopes, String patientId)
	{
		String encodedScope = Base64.getEncoder().encodeToString(scopes.getBytes());
		String encodedPatientId = Base64.getEncoder().encodeToString(patientId.getBytes());

		return actualCode + "." + encodedScope + "." + encodedPatientId;

	}
	
	public static void clearDB(IGenericClient ourClient) {
		
		//confirm that this is only being called on test data
		if (!ourClient.getServerBase().startsWith("http://localhost:" + TEST_PORT))
		{
			throw new RuntimeException("ClearDB should ONLY be used on tests!");
		}
		
		clearAllPatientsFromDB(ourClient);
		clearAllEncountersFromDB(ourClient);
	}
	
	private static void clearAllPatientsFromDB(IGenericClient ourClient)
	{
		List<BundleEntryComponent> patients = FhirUtils.getAllPatients(ourClient);
								
		for (BundleEntryComponent bundleEntryComponent : patients) {
			Patient patient = (Patient) bundleEntryComponent.getResource();
			System.out.println("Deleting Patient " + patient.getIdElement().getIdPart());
			ourClient.delete().resource(patient).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE).execute();
		}
	
	}
	
	private static void clearAllEncountersFromDB(IGenericClient ourClient)
	{
			List<BundleEntryComponent> encounters = FhirUtils.getAllEncounters(ourClient);
		
		for (BundleEntryComponent bundleEntryComponent : encounters) {
			Encounter encounter = (Encounter) bundleEntryComponent.getResource();
			System.out.println("Deleting Encounter " + encounter.getIdElement().getIdPart());
			ourClient.delete().resource(encounter).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_BEARER_VALUE).execute();
		}			
	}

}
