package org.mitre.fhir.wellknown;

import com.github.dnault.xmlpatch.internal.Log;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mitre.fhir.authorization.ServerConformanceWithAuthorizationProvider;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.mitre.fhir.utils.RsaUtils;
import org.mitre.fhir.utils.exception.RsaKeyException;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Configuration
@RestController
@CrossOrigin(origins = {"*"}, allowCredentials = "false")
public class WellKnownAuthorizationEndpointController {

  private static final String WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY = "authorization_endpoint";
  private static final String WELL_KNOWN_TOKEN_ENDPOINT_KEY = "token_endpoint";
  private static final String WELL_KNOWN_REVOCATION_ENDPOINT_KEY = "revocation_endpoint";
  private static final String WELL_KNOWN_CAPABILITIES_KEY = "capabilities";
  private static final String WELL_KNOWN_JWK_URI_KEY = "jwks_uri";
  private static final String WELL_KNOWN_GRANT_TYPES_SUPPORTED_KEY = "grant_types_supported";
  private static final String WELL_KNOWN_CODE_CHALLENGE_METHODS_SUPPORTED_KEY =
        "code_challenge_methods_supported";
  private static final String WELL_KNOWN_INTROSPECTION_ENDPOINT_KEY = "introspection_endpoint";

  // 2.1 on
  // http://hl7.org/fhir/smart-app-launch/conformance/index.html#core-capabilities
  private static final String[] capabilityValues = {
      "launch-ehr",
      "launch-standalone",
      "client-public",
      "client-confidential-asymmetric",
      "client-confidential-symmetric",
      "sso-openid-connect",
      "context-banner",
      "context-style",
      "context-ehr-patient",
      "context-ehr-encounter",
      "context-standalone-patient",
      "context-standalone-encounter",
      "permission-offline",
      "permission-patient",
      "permission-user",
      "permission-v1",
      "permission-v2",
      "authorize-post"
      };

  private static final String[] grantTypesSupportedValues = {
      "authorization_code",
      "client_credentials"
  };
  private static final String[] codeChallengeMethodsSupportedValues = {"S256"};

  private static final JSONArray WELL_KNOWN_CAPABILITIES_VALUES = new JSONArray(capabilityValues);
  private static final JSONArray WELL_KNOWN_GRANT_TYPES_SUPPORTED_VALUES =
        new JSONArray(grantTypesSupportedValues);
  private static final JSONArray WELL_KNOWN_CODE_CHALLENGE_METHODS_SUPPORTED_VALUES =
        new JSONArray(codeChallengeMethodsSupportedValues);

  private static final String[] scopes_supported = {
    "openid",
    "profile",
    "launch",
    "launch/patient",
    "offline_access",
    "patient/AllergyIntolerance.read",
    "user/AllergyIntolerance.read",
    "system/AllergyIntolerance.read",
    "patient/CarePlan.read",
    "user/CarePlan.read",
    "system/CarePlan.read",
    "patient/CareTeam.read",
    "user/CareTeam.read",
    "system/CareTeam.read",
    "patient/Condition.read",
    "user/Condition.read",
    "system/Condition.read",
    "patient/Coverage.read",
    "user/Coverage.read",
    "system/Coverage.read",
    "patient/Device.read",
    "user/Device.read",
    "system/Device.read",
    "patient/DiagnosticReport.read",
    "user/DiagnosticReport.read",
    "system/DiagnosticReport.read",
    "patient/DocumentReference.read",
    "user/DocumentReference.read",
    "system/DocumentReference.read",
    "patient/Encounter.read",
    "user/Encounter.read",
    "system/Encounter.read",
    "patient/Goal.read",
    "user/Goal.read",
    "system/Goal.read",
    "patient/Immunization.read",
    "user/Immunization.read",
    "system/Immunization.read",
    "patient/Location.read",
    "user/Location.read",
    "system/Location.read",
    "patient/Medication.read",
    "user/Medication.read",
    "system/Medication.read",
    "patient/MedicationDispense.read",
    "user/MedicationDispense.read",
    "system/MedicationDispense.read",
    "patient/MedicationRequest.read",
    "user/MedicationRequest.read",
    "system/MedicationRequest.read",
    "patient/Observation.read",
    "user/Observation.read",
    "system/Observation.read",
    "patient/Organization.read",
    "user/Organization.read",
    "system/Organization.read",
    "patient/Patient.read",
    "user/Patient.read",
    "system/Patient.read",
    "patient/Practitioner.read",
    "user/Practitioner.read",
    "system/Practitioner.read",
    "patient/PractitionerRole.read",
    "user/PractitionerRole.read",
    "system/PractitionerRole.read",
    "patient/Procedure.read",
    "user/Procedure.read",
    "system/Procedure.read",
    "patient/Provenance.read",
    "user/Provenance.read",
    "system/Provenance.read",
    "patient/QuestionnaireResponse.read",
    "user/QuestionnaireResponse.read",
    "system/QuestionnaireResponse.read",
    "patient/RelatedPerson.read",
    "user/RelatedPerson.read",
    "system/RelatedPerson.read",
    "patient/ServiceRequest.read",
    "user/ServiceRequest.read",
    "system/ServiceRequest.read",
    "patient/Specimen.read",
    "user/Specimen.read",
    "system/Specimen.read",
    "patient/AllergyIntolerance.rs",
    "user/AllergyIntolerance.rs",
    "system/AllergyIntolerance.rs",
    "patient/CarePlan.rs",
    "user/CarePlan.rs",
    "system/CarePlan.rs",
    "patient/CareTeam.rs",
    "user/CareTeam.rs",
    "system/CareTeam.rs",
    "patient/Condition.rs",
    "user/Condition.rs",
    "system/Condition.rs",
    "patient/Coverage.rs",
    "user/Coverage.rs",
    "system/Coverage.rs",
    "patient/Device.rs",
    "user/Device.rs",
    "system/Device.rs",
    "patient/DiagnosticReport.rs",
    "user/DiagnosticReport.rs",
    "system/DiagnosticReport.rs",
    "patient/DocumentReference.rs",
    "user/DocumentReference.rs",
    "system/DocumentReference.rs",
    "patient/Encounter.rs",
    "user/Encounter.rs",
    "system/Encounter.rs",
    "patient/Goal.rs",
    "user/Goal.rs",
    "system/Goal.rs",
    "patient/Immunization.rs",
    "user/Immunization.rs",
    "system/Immunization.rs",
    "patient/Location.rs",
    "user/Location.rs",
    "system/Location.rs",
    "patient/Medication.rs",
    "user/Medication.rs",
    "system/Medication.rs",
    "patient/MedicationDispense.rs",
    "user/MedicationDispense.rs",
    "system/MedicationDispense.rs",
    "patient/MedicationRequest.rs",
    "user/MedicationRequest.rs",
    "system/MedicationRequest.rs",
    "patient/Observation.rs",
    "user/Observation.rs",
    "system/Observation.rs",
    "patient/Organization.rs",
    "user/Organization.rs",
    "system/Organization.rs",
    "patient/Patient.rs",
    "user/Patient.rs",
    "system/Patient.rs",
    "patient/Practitioner.rs",
    "user/Practitioner.rs",
    "system/Practitioner.rs",
    "patient/PractitionerRole.rs",
    "user/PractitionerRole.rs",
    "system/PractitionerRole.rs",
    "patient/Procedure.rs",
    "user/Procedure.rs",
    "system/Procedure.rs",
    "patient/Provenance.rs",
    "user/Provenance.rs",
    "system/Provenance.rs",
    "patient/QuestionnaireResponse.rs",
    "user/QuestionnaireResponse.rs",
    "system/QuestionnaireResponse.rs",
    "patient/RelatedPerson.rs",
    "user/RelatedPerson.rs",
    "system/RelatedPerson.rs",
    "patient/ServiceRequest.rs",
    "user/ServiceRequest.rs",
    "system/ServiceRequest.rs",
    "patient/Specimen.rs",
    "user/Specimen.rs",
    "system/Specimen.rs",
    "patient/Condition.rs?category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern",
    "user/Condition.rs?category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern",
    "system/Condition.rs?category=http://hl7.org/fhir/us/core/CodeSystem/condition-category|health-concern",
    "patient/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis",
    "user/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis",
    "system/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|encounter-diagnosis",
    "patient/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item",
    "user/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item",
    "system/Condition.rs?category=http://terminology.hl7.org/CodeSystem/condition-category|problem-list-item",
    "patient/Observation.rs?category=http://hl7.org/fhir/us/core/CodeSystem/us-core-category|sdoh",
    "user/Observation.rs?category=http://hl7.org/fhir/us/core/CodeSystem/us-core-category|sdoh",
    "system/Observation.rs?category=http://hl7.org/fhir/us/core/CodeSystem/us-core-category|sdoh",
    "patient/Observation.rs?category=http://terminology.hl7.org//CodeSystem-observation-category|social-history",
    "user/Observation.rs?category=http://terminology.hl7.org//CodeSystem-observation-category|social-history",
    "system/Observation.rs?category=http://terminology.hl7.org//CodeSystem-observation-category|social-history",
    "patient/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory",
    "user/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory",
    "system/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|laboratory",
    "patient/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|survey",
    "user/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|survey",
    "system/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|survey",
    "patient/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|vital-signs",
    "user/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|vital-signs",
    "system/Observation.rs?category=http://terminology.hl7.org/CodeSystem/observation-category|vital-signs"
  };

  @PostConstruct
  protected void postConstruct() {
    Log.info("Well Known Authorization Controller added.");
  }

  /**
   * Get request to support well-known endpoints for authorization metadata. See
   * http://www.hl7.org/fhir/smart-app-launch/conformance/index.html#using-well-known
   *
   * @return String representing json object of metadata returned at this url
   * @throws IOException when the request fails
   */
  @GetMapping(path = "/smart-configuration", produces = {"application/json"})
  public String getWellKnownJson(HttpServletRequest theRequest) {

    JSONObject wellKnownJson = new JSONObject();
    wellKnownJson.put(WELL_KNOWN_AUTHORIZATION_ENDPOINT_KEY,
        ServerConformanceWithAuthorizationProvider.getAuthorizationExtensionUri(theRequest));
    wellKnownJson.put(WELL_KNOWN_TOKEN_ENDPOINT_KEY,
        ServerConformanceWithAuthorizationProvider.getTokenExtensionUri(theRequest));
    wellKnownJson.put(WELL_KNOWN_REVOCATION_ENDPOINT_KEY,
        ServerConformanceWithAuthorizationProvider.getRevokeExtensionUri(theRequest));
    wellKnownJson.put(WELL_KNOWN_CAPABILITIES_KEY, WELL_KNOWN_CAPABILITIES_VALUES);
    wellKnownJson.put(WELL_KNOWN_GRANT_TYPES_SUPPORTED_KEY,
        WELL_KNOWN_GRANT_TYPES_SUPPORTED_VALUES);
    wellKnownJson.put(WELL_KNOWN_CODE_CHALLENGE_METHODS_SUPPORTED_KEY,
        WELL_KNOWN_CODE_CHALLENGE_METHODS_SUPPORTED_VALUES);
    wellKnownJson.put("issuer", FhirReferenceServerUtils.getFhirServerBaseUrl(theRequest));
    wellKnownJson.put(WELL_KNOWN_JWK_URI_KEY,
        FhirReferenceServerUtils.getFhirServerBaseUrl(theRequest) + "/.well-known/jwk");
    wellKnownJson.put(WELL_KNOWN_INTROSPECTION_ENDPOINT_KEY,
        ServerConformanceWithAuthorizationProvider.getIntrospectExtensionUri(theRequest));
    wellKnownJson.put("token_endpoint_auth_methods_supported", List.of("private_key_jwt"));
    wellKnownJson.put("token_endpoint_auth_signing_alg_values_supported",
        List.of("RS384", "ES384"));
    wellKnownJson.put("scopes_supported", new JSONArray(scopes_supported));

    return wellKnownJson.toString();
  }

  /**
   * Supports retrieval of the openid configuration for authorization and authentication.
   *
   * @param theRequest the incoming HTTP request
   * @return String representing the JSON object of the OpenID configuration
   */
  @GetMapping(path = "/openid-configuration", produces = {"application/json"})
  public String getOpenIdConfiguration(HttpServletRequest theRequest) {

    String uri = FhirReferenceServerUtils.getFhirServerBaseUrl(theRequest) + "/.well-known/jwk";

    JSONObject openIdConfigJson = new JSONObject();

    openIdConfigJson.put("issuer", FhirReferenceServerUtils.getFhirServerBaseUrl(theRequest));
    openIdConfigJson.put("authorization_endpoint",
        ServerConformanceWithAuthorizationProvider.getAuthorizationExtensionUri(theRequest));
    openIdConfigJson.put("token_endpoint",
        ServerConformanceWithAuthorizationProvider.getTokenExtensionUri(theRequest));
    openIdConfigJson.put(WELL_KNOWN_JWK_URI_KEY, uri);

    String[] responseTypesSupported = {"code", "id_token", "token id_token"};
    openIdConfigJson.put("response_types_supported", new JSONArray(responseTypesSupported));

    String[] subjectTypesSupported = {"pairwise", "public"};
    openIdConfigJson.put("subject_types_supported", new JSONArray(subjectTypesSupported));

    String[] idTokenSigningAlgorithmValuesSupported = {"RS256"};
    openIdConfigJson.put("id_token_signing_alg_values_supported",
        new JSONArray(idTokenSigningAlgorithmValuesSupported));

    return openIdConfigJson.toString();

  }

  /**
   * Returns the JWK for bulk data tests.
   * @param theRequest the incoming HTTP request
   * @return String representing the JWK
   * @throws RsaKeyException When a key creation failure happens
   */
  @GetMapping(path = "/jwk", produces = {"application/json"})
  public String getJwk(HttpServletRequest theRequest) throws RsaKeyException {

    Base64.Encoder encoder = Base64.getUrlEncoder();

    RSAPublicKey publicKey = RsaUtils.getRsaPublicKey();
    byte[] modulus = publicKey.getModulus().toByteArray();
    byte[] exponent = publicKey.getPublicExponent().toByteArray();

    String algorithm = "RS256";
    String kty = "RSA";
    String use = "sig";

    String encodedModulus = encoder.encodeToString(modulus);
    String encodedExponent = encoder.encodeToString(exponent);

    JSONObject firstKeyEntry = new JSONObject();
    firstKeyEntry.put("alg", algorithm);
    firstKeyEntry.put("kty", kty);
    firstKeyEntry.put("use", use);
    firstKeyEntry.put("n", encodedModulus);
    firstKeyEntry.put("e", encodedExponent);

    JSONArray keys = new JSONArray();
    keys.put(firstKeyEntry);

    JSONObject jwk = new JSONObject();
    jwk.put("keys", keys);

    return jwk.toString();

  }
}
