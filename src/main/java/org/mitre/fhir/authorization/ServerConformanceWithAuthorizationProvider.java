package org.mitre.fhir.authorization;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementImplementationComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementKind;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.RestfulCapabilityMode;
import org.hl7.fhir.r4.model.CapabilityStatement.TypeRestfulInteraction;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations.FHIRVersion;
import org.hl7.fhir.r4.model.Enumerations.PublicationStatus;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.UriType;
import org.mitre.fhir.utils.FhirReferenceServerUtils;

public class ServerConformanceWithAuthorizationProvider extends JpaConformanceProviderR4 {

  public static final String TOKEN_EXTENSION_URL = "token";
  public static final String AUTHORIZE_EXTENSION_URL = "authorize";
  public static final String REVOKE_EXTENSION_URL = "revoke";
  private static final String OAUTH_URL =
      "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";
  private static final String TOKEN_EXTENSION_VALUE_URI = "/oauth/token";
  private static final String AUTHORIZE_EXTENSION_VALUE_URI = "/oauth/authorization";
  private static final String REVOKE_EXTENSION_VALUE_URI = "/oauth/token/revoke-token";

  private static final String SEARCH_REV_INCLUDE = "Provenance:target";

  private static final String PROFILE_PREFIX = "http://hl7.org/fhir/us/core/StructureDefinition/";

  private static final String ALLERGY_INTOLERANCE = "AllergyIntolerance";
  private static final String CARE_PLAN = "CarePlan";
  private static final String CARE_TEAM = "CareTeam";
  private static final String CONDITION = "Condition";
  private static final String DEVICE = "Device";
  private static final String DIAGNOSTIC_REPORT = "DiagnosticReport";
  private static final String DOCUMENT_REFERENCE = "DocumentReference";
  private static final String ENCOUNTER = "Encounter";
  private static final String GOAL = "Goal";
  private static final String IMMUNIZATION = "Immunization";
  private static final String LOCATION = "Location";
  private static final String MEDICATION = "Medication";
  private static final String MEDICATION_REQUEST = "MedicationRequest";
  private static final String OBSERVATION = "Observation";
  private static final String ORGANIZATION = "Organization";
  private static final String PATIENT = "Patient";
  private static final String PRACTITIONER = "Practitioner";
  private static final String PRACTITIONER_ROLE = "PractitionerRole";
  private static final String PROCEDURE = "Procedure";
  private static final String PROVENANCE = "Provenance";

  public ServerConformanceWithAuthorizationProvider(RestfulServer theRestfulServer,
      IFhirSystemDao<Bundle, Meta> theSystemDao, DaoConfig theDaoConfig,
      ISearchParamRegistry searchParamRegistry) {
    super(theRestfulServer, theSystemDao, theDaoConfig, searchParamRegistry);
  }

  public static String getTokenExtensionUri(HttpServletRequest theRequest) {
    return FhirReferenceServerUtils.getServerBaseUrl(theRequest) + TOKEN_EXTENSION_VALUE_URI;
  }

  public static String getAuthorizationExtensionUri(HttpServletRequest theRequest) {
    return FhirReferenceServerUtils.getServerBaseUrl(theRequest) + AUTHORIZE_EXTENSION_VALUE_URI;
  }

  public static String getRevokeExtensionUri(HttpServletRequest theRequest) {
    return FhirReferenceServerUtils.getServerBaseUrl(theRequest) + REVOKE_EXTENSION_VALUE_URI;
  }

  private Extension getOauthUris(HttpServletRequest theRequest) {
    Extension oauthUris = new Extension();
    oauthUris.setUrl(OAUTH_URL); // url

    oauthUris.addExtension(
        new Extension(TOKEN_EXTENSION_URL, new UriType(getTokenExtensionUri(theRequest))));

    oauthUris.addExtension(new Extension(AUTHORIZE_EXTENSION_URL,
        new UriType(getAuthorizationExtensionUri(theRequest))));

    oauthUris.addExtension(
        new Extension(REVOKE_EXTENSION_URL, new UriType(getRevokeExtensionUri(theRequest))));

    return oauthUris;
  }

  private CapabilityStatementRestSecurityComponent getSecurity(HttpServletRequest theRequest) {
    CapabilityStatementRestSecurityComponent security =
        new CapabilityStatementRestSecurityComponent();
    Extension oauthUris = getOauthUris(theRequest);
    security.addExtension(oauthUris);

    CodeableConcept service = security.addService();
    Coding coding = service.addCoding();
    coding.setSystem("http://hl7.org/fhir/ValueSet/restful-security-service");
    coding.setCode("SMART-on-FHIR");

    service.setText("OAuth2 using SMART-on-FHIR profile (see http://docs.smarthealthit.org)");
    return security;
  }

  /**
   * All resources are hardcode into this method as each has its own considerations.
   * 
   * @return List of Capability Statement resources with expected attributes for Inferno Testing
   */
  private List<CapabilityStatementRestResourceComponent> getResources() {

    CapabilityStatementRestResourceComponent allergyIntolerance =
        new CapabilityStatementRestResourceComponent();
    allergyIntolerance.setType(ALLERGY_INTOLERANCE);
    allergyIntolerance.setProfile(getProfile(ALLERGY_INTOLERANCE));
    allergyIntolerance.addSupportedProfile(getProfile("us-core-allergyintolerance"));
    allergyIntolerance.addSearchParam().setName("clinical-status").setType(SearchParamType.TOKEN);
    allergyIntolerance.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    allergyIntolerance.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    allergyIntolerance.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    allergyIntolerance.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    allergyIntolerance.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    allergyIntolerance.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    List<CapabilityStatementRestResourceComponent> resources = new ArrayList<>();
    resources.add(allergyIntolerance);

    CapabilityStatementRestResourceComponent carePlan =
        new CapabilityStatementRestResourceComponent();
    carePlan.setType(CARE_PLAN);
    carePlan.setProfile(getProfile(CARE_PLAN));
    carePlan.addSupportedProfile(getProfile("us-core-careplan"));
    carePlan.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    carePlan.addSearchParam().setName("date").setType(SearchParamType.DATE);
    carePlan.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    carePlan.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    carePlan.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    carePlan.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    carePlan.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    carePlan.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(carePlan);

    CapabilityStatementRestResourceComponent careTeam =
        new CapabilityStatementRestResourceComponent();
    careTeam.setType(CARE_TEAM);
    careTeam.setProfile(getProfile(CARE_TEAM));
    careTeam.addSupportedProfile(getProfile("us-core-careteam"));
    careTeam.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    careTeam.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    careTeam.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    careTeam.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    careTeam.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    careTeam.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    careTeam.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(careTeam);

    CapabilityStatementRestResourceComponent condition =
        new CapabilityStatementRestResourceComponent();
    condition.setType(CONDITION);
    condition.setProfile(getProfile(CONDITION));
    condition.addSupportedProfile(getProfile("us-core-condition"));
    condition.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("clinical-status").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    condition.addSearchParam().setName("onset-date").setType(SearchParamType.DATE);
    condition.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    condition.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    condition.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    condition.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    condition.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    condition.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(condition);

    CapabilityStatementRestResourceComponent device =
        new CapabilityStatementRestResourceComponent();
    device.setType(DEVICE);
    device.setProfile(getProfile(DEVICE));
    device.addSupportedProfile(getProfile("us-core-implantable-device"));
    device.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    device.addSearchParam().setName("type").setType(SearchParamType.TOKEN);
    device.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    device.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    device.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    device.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    device.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(device);

    CapabilityStatementRestResourceComponent diagnosticReport =
        new CapabilityStatementRestResourceComponent();
    diagnosticReport.setType(DIAGNOSTIC_REPORT);
    diagnosticReport.setProfile(getProfile(DIAGNOSTIC_REPORT));
    diagnosticReport.addSupportedProfile(getProfile("us-core-diagnosticreport-lab"));
    diagnosticReport.addSupportedProfile(getProfile("us-core-diagnosticreport-note"));
    diagnosticReport.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    diagnosticReport.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    diagnosticReport.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    diagnosticReport.addSearchParam().setName("date").setType(SearchParamType.DATE);
    diagnosticReport.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    diagnosticReport.addInteraction().setCode(TypeRestfulInteraction.CREATE);
    diagnosticReport.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    diagnosticReport.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    diagnosticReport.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    diagnosticReport.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(diagnosticReport);

    CapabilityStatementRestResourceComponent documentReference =
        new CapabilityStatementRestResourceComponent();
    documentReference.setType(DOCUMENT_REFERENCE);
    documentReference.setProfile(getProfile(DOCUMENT_REFERENCE));
    documentReference.addSupportedProfile(getProfile("us-core-documentreference"));
    documentReference.addSearchParam().setName("_id").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("type").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("date").setType(SearchParamType.DATE);
    documentReference.addSearchParam().setName("period").setType(SearchParamType.DATE);
    documentReference.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    documentReference.addInteraction().setCode(TypeRestfulInteraction.CREATE);
    documentReference.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    documentReference.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    documentReference.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    documentReference.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(documentReference);

    CapabilityStatementRestResourceComponent encounter =
        new CapabilityStatementRestResourceComponent();
    encounter.setType(ENCOUNTER);
    encounter.setProfile(getProfile(ENCOUNTER));
    encounter.addSupportedProfile(getProfile("us-core-encounter"));
    // SHALL
    encounter.addSearchParam().setName("_id").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("class").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("date").setType(SearchParamType.DATE);
    encounter.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("type").setType(SearchParamType.TOKEN);
    encounter.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    encounter.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    encounter.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    encounter.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    encounter.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(encounter);

    CapabilityStatementRestResourceComponent goal = new CapabilityStatementRestResourceComponent();
    goal.setType(GOAL);
    goal.setProfile(getProfile(GOAL));
    goal.addSupportedProfile(getProfile("us-core-goal"));
    goal.addSearchParam().setName("lifecycle-status").setType(SearchParamType.TOKEN);
    goal.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    goal.addSearchParam().setName("target-date").setType(SearchParamType.DATE);
    goal.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    goal.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    goal.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    goal.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    goal.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(goal);

    CapabilityStatementRestResourceComponent immunization =
        new CapabilityStatementRestResourceComponent();
    immunization.setType(IMMUNIZATION);
    immunization.setProfile(getProfile(IMMUNIZATION));
    immunization.addSupportedProfile(getProfile("us-core-immunization"));
    immunization.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    immunization.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    immunization.addSearchParam().setName("date").setType(SearchParamType.DATE);
    immunization.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    immunization.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    immunization.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    immunization.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    immunization.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(immunization);

    CapabilityStatementRestResourceComponent location =
        new CapabilityStatementRestResourceComponent();
    location.setType(LOCATION);
    location.setProfile(getProfile(LOCATION));
    location.addSupportedProfile(getProfile("us-core-location"));
    // SHALL
    location.addSearchParam().setName("name").setType(SearchParamType.STRING);
    location.addSearchParam().setName("address").setType(SearchParamType.STRING);
    // SHOULD
    location.addSearchParam().setName("address-city").setType(SearchParamType.STRING);
    location.addSearchParam().setName("address-state").setType(SearchParamType.STRING);
    location.addSearchParam().setName("address-postalcode").setType(SearchParamType.STRING);
    // SHALL
    location.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    location.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    location.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    location.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(location);

    CapabilityStatementRestResourceComponent medication =
        new CapabilityStatementRestResourceComponent();
    medication.setType(MEDICATION);
    medication.setProfile(getProfile(MEDICATION));
    medication.addSupportedProfile(getProfile("us-core-medication"));
    // SHALL
    medication.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    medication.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    medication.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(medication);

    CapabilityStatementRestResourceComponent medicationRequest =
        new CapabilityStatementRestResourceComponent();
    medicationRequest.setType(MEDICATION_REQUEST);
    medicationRequest.setProfile(getProfile(MEDICATION_REQUEST));
    medicationRequest.addSupportedProfile(getProfile("us-core-medicationrequest"));
    medicationRequest.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("intent").setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchParam().setName("authoredon").setType(SearchParamType.DATE);
    medicationRequest.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    medicationRequest.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    medicationRequest.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    medicationRequest.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    medicationRequest.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(medicationRequest);

    CapabilityStatementRestResourceComponent observation =
        new CapabilityStatementRestResourceComponent();
    observation.setType(OBSERVATION);
    observation.setProfile(getProfile(OBSERVATION));
    observation.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("date").setType(SearchParamType.DATE);
    observation.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    observation.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    observation.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    observation.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    observation.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    observation.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(observation);

    CapabilityStatementRestResourceComponent organization =
        new CapabilityStatementRestResourceComponent();
    organization.setType(ORGANIZATION);
    organization.setProfile(getProfile(ORGANIZATION));
    organization.addSupportedProfile(getProfile("us-core-organization"));
    organization.addSearchParam().setName("name").setType(SearchParamType.STRING);
    organization.addSearchParam().setName("address").setType(SearchParamType.STRING);
    // SHALL
    organization.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    organization.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    organization.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    organization.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(organization);

    CapabilityStatementRestResourceComponent patient =
        new CapabilityStatementRestResourceComponent();
    patient.setType(PATIENT);
    patient.setProfile(getProfile(PATIENT));
    patient.addSupportedProfile(getProfile("us-core-patient"));
    patient.addSearchParam().setName("_id").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("birthdate").setType(SearchParamType.DATE);
    patient.addSearchParam().setName("family").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("gender").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("given").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("name").setType(SearchParamType.STRING);
    patient.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    patient.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    patient.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    patient.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    patient.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(patient);

    CapabilityStatementRestResourceComponent practitioner =
        new CapabilityStatementRestResourceComponent();
    practitioner.setType(PRACTITIONER);
    practitioner.setProfile(getProfile(PRACTITIONER));
    practitioner.addSupportedProfile(getProfile("us-core-practitioner"));
    practitioner.addSearchParam().setName("name").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    // SHALL
    practitioner.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    practitioner.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    practitioner.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    practitioner.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(practitioner);

    CapabilityStatementRestResourceComponent practitionerRole =
        new CapabilityStatementRestResourceComponent();
    practitionerRole.setType(PRACTITIONER_ROLE);
    practitionerRole.setProfile(getProfile(PRACTITIONER_ROLE));
    practitionerRole.addSupportedProfile(getProfile("us-core-practitionerrole"));
    practitionerRole.addSearchParam().setName("specialty").setType(SearchParamType.TOKEN);
    practitionerRole.addSearchParam().setName("practitioner").setType(SearchParamType.REFERENCE);
    // SHALL
    practitionerRole.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    practitionerRole.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    practitionerRole.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    practitionerRole.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(practitionerRole);

    CapabilityStatementRestResourceComponent procedure =
        new CapabilityStatementRestResourceComponent();
    procedure.setType(PROCEDURE);
    procedure.setProfile(getProfile(PROCEDURE));
    procedure.addSupportedProfile(getProfile("us-core-procedure"));
    procedure.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    procedure.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("date").setType(SearchParamType.DATE);
    procedure.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    procedure.addSearchRevInclude(SEARCH_REV_INCLUDE);
    // SHALL
    procedure.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    procedure.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    procedure.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    procedure.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(procedure);

    CapabilityStatementRestResourceComponent provenance =
        new CapabilityStatementRestResourceComponent();
    provenance.setType(PROVENANCE);
    provenance.setProfile(getProfile(PROVENANCE));
    provenance.addSupportedProfile(getProfile("us-core-provenance"));
    // SHALL
    provenance.addInteraction().setCode(TypeRestfulInteraction.READ);
    // SHOULD
    provenance.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    provenance.addInteraction().setCode(TypeRestfulInteraction.HISTORYINSTANCE);
    resources.add(provenance);

    return resources;
  }

  private static String getProfile(String profileName) {
    return PROFILE_PREFIX + "/" + profileName + "/";
  }


  @Override
  public CapabilityStatement getServerConformance(HttpServletRequest theRequest,
      RequestDetails theRequestDetails) {

    CapabilityStatement capabilityStatement = new CapabilityStatement();
    capabilityStatement.setStatus(PublicationStatus.ACTIVE);
    capabilityStatement.setDate(new Date());
    capabilityStatement.setKind(CapabilityStatementKind.INSTANCE);

    CapabilityStatementImplementationComponent implementation =
        new CapabilityStatementImplementationComponent();
    implementation.setDescription(
        "Inferno Reference Server for US Core Implementation Guide v3.1.1 based on HAPI FHIR R4 "
            + " Server");
    implementation.setUrl(FhirReferenceServerUtils.getServerBaseUrl(theRequest));
    capabilityStatement.setImplementation(implementation);

    capabilityStatement.setFhirVersion(FHIRVersion._4_0_1);

    capabilityStatement.addFormat("application/fhir+xml");
    capabilityStatement.addFormat("application/fhir+json");

    capabilityStatement.setPublisher("MITRE");

    CanonicalType implementationGuide = new CanonicalType();
    implementationGuide
        .setValue("http://hl7.org/fhir/us/core/ImplementationGuide/hl7.fhir.us.core|3.1.1");
    List<CanonicalType> implementationGuides = new ArrayList<>();
    implementationGuides.add(implementationGuide);
    capabilityStatement.setImplementationGuide(implementationGuides);

    CapabilityStatementRestComponent rest = capabilityStatement.addRest();
    CapabilityStatementRestSecurityComponent security = getSecurity(theRequest);
    rest.setSecurity(security);
    rest.setMode(RestfulCapabilityMode.SERVER);

    List<CapabilityStatementRestResourceComponent> resources = getResources();
    for (CapabilityStatementRestResourceComponent resource : resources) {
      rest.addResource(resource);
    }

    return capabilityStatement;
  }
}
