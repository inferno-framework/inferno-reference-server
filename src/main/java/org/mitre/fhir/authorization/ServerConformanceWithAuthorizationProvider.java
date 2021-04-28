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
    allergyIntolerance.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    allergyIntolerance.addSearchRevInclude(SEARCH_REV_INCLUDE);
    allergyIntolerance.addInteraction().setCode(TypeRestfulInteraction.READ);
    allergyIntolerance.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    allergyIntolerance.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);

    List<CapabilityStatementRestResourceComponent> resources = new ArrayList<>();
    resources.add(allergyIntolerance);

    CapabilityStatementRestResourceComponent carePlan =
        new CapabilityStatementRestResourceComponent();
    carePlan.setType(CARE_PLAN);
    carePlan.setProfile(getProfile(CARE_PLAN));
    carePlan.addSupportedProfile(getProfile("us-core-careplan"));
    carePlan.addSearchParam().setName("activity-code").setType(SearchParamType.TOKEN);
    carePlan.addSearchParam().setName("activity-date").setType(SearchParamType.DATE);
    carePlan.addSearchParam().setName("activity-reference").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("based-on").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("care-team").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    carePlan.addSearchParam().setName("condition").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("date").setType(SearchParamType.DATE);
    carePlan.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("goal").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    carePlan.addSearchParam().setName("instantiates-canonical").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("instantiates-uri").setType(SearchParamType.URI);
    carePlan.addSearchParam().setName("intent").setType(SearchParamType.TOKEN);
    carePlan.addSearchParam().setName("part-of").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("performer").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("replaces").setType(SearchParamType.REFERENCE);
    carePlan.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    carePlan.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    carePlan.addSearchRevInclude(SEARCH_REV_INCLUDE);
    carePlan.addInteraction().setCode(TypeRestfulInteraction.READ);
    carePlan.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    carePlan.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(carePlan);

    CapabilityStatementRestResourceComponent careTeam =
        new CapabilityStatementRestResourceComponent();
    careTeam.setType(CARE_TEAM);
    careTeam.setProfile(getProfile(CARE_TEAM));
    careTeam.addSupportedProfile(getProfile("us-core-careteam"));
    careTeam.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    careTeam.addSearchParam().setName("date").setType(SearchParamType.DATE);
    careTeam.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    careTeam.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    careTeam.addSearchParam().setName("participant").setType(SearchParamType.REFERENCE);
    careTeam.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    careTeam.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    careTeam.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    careTeam.addSearchRevInclude(SEARCH_REV_INCLUDE);
    careTeam.addInteraction().setCode(TypeRestfulInteraction.READ);
    careTeam.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    careTeam.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(careTeam);

    CapabilityStatementRestResourceComponent condition =
        new CapabilityStatementRestResourceComponent();
    condition.setType(CONDITION);
    condition.setProfile(getProfile(CONDITION));
    condition.addSupportedProfile(getProfile("us-core-condition"));
    condition.addSearchParam().setName("abatement-age").setType(SearchParamType.QUANTITY);
    condition.addSearchParam().setName("abatement-date").setType(SearchParamType.DATE);
    condition.addSearchParam().setName("abatement-string").setType(SearchParamType.STRING);
    condition.addSearchParam().setName("asserter").setType(SearchParamType.REFERENCE);
    condition.addSearchParam().setName("body-site").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("clinical-status").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    condition.addSearchParam().setName("evidence").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("evidence-detail").setType(SearchParamType.REFERENCE);
    condition.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("onset-age").setType(SearchParamType.QUANTITY);
    condition.addSearchParam().setName("onset-date").setType(SearchParamType.DATE);
    condition.addSearchParam().setName("onset-info").setType(SearchParamType.STRING);
    condition.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    condition.addSearchParam().setName("recorded-date").setType(SearchParamType.DATE);
    condition.addSearchParam().setName("severity").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("stage").setType(SearchParamType.TOKEN);
    condition.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    condition.addSearchParam().setName("verification-status").setType(SearchParamType.TOKEN);
    condition.addSearchRevInclude(SEARCH_REV_INCLUDE);
    condition.addInteraction().setCode(TypeRestfulInteraction.READ);
    condition.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    condition.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(condition);

    CapabilityStatementRestResourceComponent device =
        new CapabilityStatementRestResourceComponent();
    device.setType(DEVICE);
    device.setProfile(getProfile(DEVICE));
    device.addSupportedProfile(getProfile("us-core-implantable-device"));
    device.addSearchParam().setName("device-name").setType(SearchParamType.STRING);
    device.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    device.addSearchParam().setName("location").setType(SearchParamType.REFERENCE);
    device.addSearchParam().setName("manufacturer").setType(SearchParamType.STRING);
    device.addSearchParam().setName("model").setType(SearchParamType.STRING);
    device.addSearchParam().setName("organization").setType(SearchParamType.REFERENCE);
    device.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    device.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    device.addSearchParam().setName("type").setType(SearchParamType.TOKEN);
    device.addSearchParam().setName("udi-carrier").setType(SearchParamType.STRING);
    device.addSearchParam().setName("udi-di").setType(SearchParamType.STRING);
    device.addSearchParam().setName("url").setType(SearchParamType.URI);
    device.addSearchRevInclude(SEARCH_REV_INCLUDE);
    device.addInteraction().setCode(TypeRestfulInteraction.READ);
    device.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    device.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(device);

    CapabilityStatementRestResourceComponent diagnosticReport =
        new CapabilityStatementRestResourceComponent();
    diagnosticReport.setType(DIAGNOSTIC_REPORT);
    diagnosticReport.setProfile(getProfile(DIAGNOSTIC_REPORT));
    diagnosticReport.addSupportedProfile(getProfile("us-core-diagnosticreport-lab"));
    diagnosticReport.addSupportedProfile(getProfile("us-core-diagnosticreport-note"));
    diagnosticReport.addSearchParam().setName("based-on").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    diagnosticReport.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    diagnosticReport.addSearchParam().setName("conclusion").setType(SearchParamType.TOKEN);
    diagnosticReport.addSearchParam().setName("date").setType(SearchParamType.DATE);
    diagnosticReport.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    diagnosticReport.addSearchParam().setName("issued").setType(SearchParamType.DATE);
    diagnosticReport.addSearchParam().setName("media").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("performer").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("result").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("results-interpreter")
        .setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("specimen").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    diagnosticReport.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    diagnosticReport.addSearchRevInclude(SEARCH_REV_INCLUDE);
    diagnosticReport.addInteraction().setCode(TypeRestfulInteraction.READ);
    diagnosticReport.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    diagnosticReport.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(diagnosticReport);

    CapabilityStatementRestResourceComponent documentReference =
        new CapabilityStatementRestResourceComponent();
    documentReference.setType(DOCUMENT_REFERENCE);
    documentReference.setProfile(getProfile(DOCUMENT_REFERENCE));
    documentReference.addSupportedProfile(getProfile("us-core-documentreference"));
    documentReference.addSearchParam().setName("authenticator").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("author").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("contenttype").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("custodian").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("date").setType(SearchParamType.DATE);
    documentReference.addSearchParam().setName("description").setType(SearchParamType.STRING);
    documentReference.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("event").setType(SearchParamType.STRING);
    documentReference.addSearchParam().setName("facility").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("format").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("language").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("location").setType(SearchParamType.URI);
    documentReference.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("period").setType(SearchParamType.DATE);
    documentReference.addSearchParam().setName("related").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("relatesto").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("relationship").setType(SearchParamType.COMPOSITE);
    documentReference.addSearchParam().setName("security-label").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("setting").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    documentReference.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    documentReference.addSearchParam().setName("type").setType(SearchParamType.TOKEN);
    documentReference.addSearchRevInclude(SEARCH_REV_INCLUDE);
    documentReference.addInteraction().setCode(TypeRestfulInteraction.READ);
    documentReference.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    documentReference.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(documentReference);

    CapabilityStatementRestResourceComponent encounter =
        new CapabilityStatementRestResourceComponent();
    encounter.setType(ENCOUNTER);
    encounter.setProfile(getProfile(ENCOUNTER));
    encounter.addSupportedProfile(getProfile("us-core-encounter"));
    encounter.addSearchParam().setName("account").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("appointment").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("based-on").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("class").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("date").setType(SearchParamType.DATE);
    encounter.addSearchParam().setName("diagnosis").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("episode-of-care").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("length").setType(SearchParamType.QUANTITY);
    encounter.addSearchParam().setName("location").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("location-period").setType(SearchParamType.DATE);
    encounter.addSearchParam().setName("part-of").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("participant").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("participant-type").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("practitioner").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("reason-code").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("reason-reference").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("service-provider").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("special-arrangement").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    encounter.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    encounter.addSearchParam().setName("type").setType(SearchParamType.TOKEN);
    encounter.addSearchRevInclude(SEARCH_REV_INCLUDE);
    encounter.addInteraction().setCode(TypeRestfulInteraction.READ);
    encounter.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    encounter.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(encounter);

    CapabilityStatementRestResourceComponent goal = new CapabilityStatementRestResourceComponent();
    goal.setType(GOAL);
    goal.setProfile(getProfile(GOAL));
    goal.addSupportedProfile(getProfile("us-core-goal"));
    goal.addSearchParam().setName("achievement-status").setType(SearchParamType.TOKEN);
    goal.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    goal.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    goal.addSearchParam().setName("lifecycle-status").setType(SearchParamType.TOKEN);
    goal.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    goal.addSearchParam().setName("start-date").setType(SearchParamType.DATE);
    goal.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    goal.addSearchParam().setName("target-date").setType(SearchParamType.DATE);
    goal.addSearchRevInclude(SEARCH_REV_INCLUDE);
    goal.addInteraction().setCode(TypeRestfulInteraction.READ);
    goal.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    goal.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(goal);

    CapabilityStatementRestResourceComponent immunization =
        new CapabilityStatementRestResourceComponent();
    immunization.setType(IMMUNIZATION);
    immunization.setProfile(getProfile(IMMUNIZATION));
    immunization.addSupportedProfile(getProfile("us-core-immunization"));
    immunization.addSearchParam().setName("date").setType(SearchParamType.DATE);
    immunization.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    immunization.addSearchParam().setName("location").setType(SearchParamType.REFERENCE);
    immunization.addSearchParam().setName("lot-number").setType(SearchParamType.STRING);
    immunization.addSearchParam().setName("manufacturer").setType(SearchParamType.REFERENCE);
    immunization.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    immunization.addSearchParam().setName("performer").setType(SearchParamType.REFERENCE);
    immunization.addSearchParam().setName("reaction").setType(SearchParamType.REFERENCE);
    immunization.addSearchParam().setName("reaction-date").setType(SearchParamType.DATE);
    immunization.addSearchParam().setName("reason-code").setType(SearchParamType.TOKEN);
    immunization.addSearchParam().setName("reason-reference").setType(SearchParamType.REFERENCE);
    immunization.addSearchParam().setName("series").setType(SearchParamType.STRING);
    immunization.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    immunization.addSearchParam().setName("status-reason").setType(SearchParamType.TOKEN);
    immunization.addSearchParam().setName("target-disease").setType(SearchParamType.TOKEN);
    immunization.addSearchParam().setName("vaccine-code").setType(SearchParamType.TOKEN);
    immunization.addSearchRevInclude(SEARCH_REV_INCLUDE);
    immunization.addInteraction().setCode(TypeRestfulInteraction.READ);
    immunization.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    immunization.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(immunization);

    CapabilityStatementRestResourceComponent location =
        new CapabilityStatementRestResourceComponent();
    location.setType(LOCATION);
    location.setProfile(getProfile(LOCATION));
    location.addSupportedProfile(getProfile("us-core-location"));
    location.addSearchParam().setName("address").setType(SearchParamType.STRING);
    location.addSearchParam().setName("address-city").setType(SearchParamType.STRING);
    location.addSearchParam().setName("address-country").setType(SearchParamType.STRING);
    location.addSearchParam().setName("address-postalcode").setType(SearchParamType.STRING);
    location.addSearchParam().setName("address-state").setType(SearchParamType.STRING);
    location.addSearchParam().setName("address-use").setType(SearchParamType.TOKEN);
    location.addSearchParam().setName("endpoint").setType(SearchParamType.REFERENCE);
    location.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    location.addSearchParam().setName("name").setType(SearchParamType.STRING);
    location.addSearchParam().setName("near").setType(SearchParamType.SPECIAL);
    location.addSearchParam().setName("operational-status").setType(SearchParamType.TOKEN);
    location.addSearchParam().setName("organization").setType(SearchParamType.REFERENCE);
    location.addSearchParam().setName("partof").setType(SearchParamType.REFERENCE);
    location.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    location.addSearchParam().setName("type").setType(SearchParamType.TOKEN);
    location.addSearchRevInclude(SEARCH_REV_INCLUDE);
    location.addInteraction().setCode(TypeRestfulInteraction.READ);
    location.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    location.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(location);

    CapabilityStatementRestResourceComponent medication =
        new CapabilityStatementRestResourceComponent();
    medication.setType(MEDICATION);
    medication.setProfile(getProfile(MEDICATION));
    medication.addSupportedProfile(getProfile("us-core-medication"));
    medication.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    medication.addSearchParam().setName("expiration-date").setType(SearchParamType.DATE);
    medication.addSearchParam().setName("form").setType(SearchParamType.TOKEN);
    medication.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    medication.addSearchParam().setName("ingredient").setType(SearchParamType.REFERENCE);
    medication.addSearchParam().setName("ingredient-code").setType(SearchParamType.TOKEN);
    medication.addSearchParam().setName("lot-number").setType(SearchParamType.TOKEN);
    medication.addSearchParam().setName("manufacturer").setType(SearchParamType.REFERENCE);
    medication.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    medication.addSearchRevInclude(SEARCH_REV_INCLUDE);
    medication.addInteraction().setCode(TypeRestfulInteraction.READ);
    medication.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    medication.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(medication);

    CapabilityStatementRestResourceComponent medicationRequest =
        new CapabilityStatementRestResourceComponent();
    medicationRequest.setType(MEDICATION_REQUEST);
    medicationRequest.setProfile(getProfile(MEDICATION_REQUEST));
    medicationRequest.addSupportedProfile(getProfile("us-core-medicationrequest"));
    medicationRequest.addSearchParam().setName("authoredon").setType(SearchParamType.DATE);
    medicationRequest.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("date").setType(SearchParamType.DATE);
    medicationRequest.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("intended-dispenser")
        .setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchParam().setName("intended-performer")
        .setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchParam().setName("intended-performertype")
        .setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("intent").setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("medication").setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchParam().setName("priority").setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("requester").setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    medicationRequest.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    medicationRequest.addSearchRevInclude(SEARCH_REV_INCLUDE);
    medicationRequest.addInteraction().setCode(TypeRestfulInteraction.READ);
    medicationRequest.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    medicationRequest.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(medicationRequest);

    CapabilityStatementRestResourceComponent observation =
        new CapabilityStatementRestResourceComponent();
    observation.setType(OBSERVATION);
    observation.setProfile(getProfile(OBSERVATION));
    observation.addSupportedProfile(getProfile("us-core-observation-lab"));
    observation.addSupportedProfile(getProfile("pediatric-bmi-for-age"));
    observation.addSupportedProfile(getProfile("pediatric-weight-for-height"));
    observation.addSupportedProfile(getProfile("us-core-pulse-oximetry"));
    observation.addSupportedProfile(getProfile("us-core-smokingstatus"));
    observation.addSupportedProfile(getProfile("vitalsigns"));
    observation.addSupportedProfile(getProfile("bodyheight"));
    observation.addSupportedProfile(getProfile("heartrate"));
    observation.addSupportedProfile(getProfile("bodyweight"));
    observation.addSupportedProfile(getProfile("head-occipital-frontal-circumference-percentile"));
    observation.addSupportedProfile(getProfile("resprate"));
    observation.addSupportedProfile(getProfile("bodytemp"));
    observation.addSupportedProfile(getProfile("bp"));
    observation.addSearchParam().setName("based-on").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("code-value-concept").setType(SearchParamType.COMPOSITE);
    observation.addSearchParam().setName("code-value-date").setType(SearchParamType.COMPOSITE);
    observation.addSearchParam().setName("code-value-quantity").setType(SearchParamType.COMPOSITE);
    observation.addSearchParam().setName("code-value-string").setType(SearchParamType.COMPOSITE);
    observation.addSearchParam().setName("combo-code").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("combo-code-value-concept")
        .setType(SearchParamType.COMPOSITE);
    observation.addSearchParam().setName("combo-code-value-quantity")
        .setType(SearchParamType.QUANTITY);
    observation.addSearchParam().setName("combo-data-absent-reason").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("combo-value-concept").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("combo-value-quantity").setType(SearchParamType.QUANTITY);
    observation.addSearchParam().setName("component-code").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("component-code-value-concept")
        .setType(SearchParamType.COMPOSITE);
    observation.addSearchParam().setName("component-code-value-quantity")
        .setType(SearchParamType.COMPOSITE);
    observation.addSearchParam().setName("component-data-absent-reason")
        .setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("component-value-concept").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("component-value-quantity")
        .setType(SearchParamType.QUANTITY);
    observation.addSearchParam().setName("data-absent-reason").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("date").setType(SearchParamType.DATE);
    observation.addSearchParam().setName("derived-from").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("device").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("focus").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("has-member").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("method").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("part-of").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("performer").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("specimen").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("value-concept").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("value-date").setType(SearchParamType.DATE);
    observation.addSearchParam().setName("value-quantity").setType(SearchParamType.QUANTITY);
    observation.addSearchParam().setName("value-string").setType(SearchParamType.STRING);
    observation.addSearchRevInclude(SEARCH_REV_INCLUDE);
    observation.addInteraction().setCode(TypeRestfulInteraction.READ);
    observation.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    observation.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(observation);

    CapabilityStatementRestResourceComponent organization =
        new CapabilityStatementRestResourceComponent();
    organization.setType(ORGANIZATION);
    organization.setProfile(getProfile(ORGANIZATION));
    organization.addSupportedProfile(getProfile("us-core-organization"));
    observation.addSearchParam().setName("active").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("address").setType(SearchParamType.STRING);
    observation.addSearchParam().setName("address-city").setType(SearchParamType.STRING);
    observation.addSearchParam().setName("address-country").setType(SearchParamType.STRING);
    observation.addSearchParam().setName("address-postalcode").setType(SearchParamType.STRING);
    observation.addSearchParam().setName("address-state").setType(SearchParamType.STRING);
    observation.addSearchParam().setName("address-use").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("endpoint").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    observation.addSearchParam().setName("name").setType(SearchParamType.STRING);
    observation.addSearchParam().setName("partof").setType(SearchParamType.REFERENCE);
    observation.addSearchParam().setName("phonetic").setType(SearchParamType.STRING);
    observation.addSearchParam().setName("type").setType(SearchParamType.TOKEN);
    organization.addSearchRevInclude(SEARCH_REV_INCLUDE);
    organization.addInteraction().setCode(TypeRestfulInteraction.READ);
    organization.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    organization.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(organization);

    CapabilityStatementRestResourceComponent patient =
        new CapabilityStatementRestResourceComponent();
    patient.setType(PATIENT);
    patient.setProfile(getProfile(PATIENT));
    patient.addSupportedProfile(getProfile("us-core-patient"));
    patient.addSearchParam().setName("active").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("address").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("address-city").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("address-country").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("address-postalcode").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("address-state").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("address-use").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("birthdate").setType(SearchParamType.DATE);
    patient.addSearchParam().setName("death-date").setType(SearchParamType.DATE);
    patient.addSearchParam().setName("deceased").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("email").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("family").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("gender").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("general-practitioner").setType(SearchParamType.REFERENCE);
    patient.addSearchParam().setName("given").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("_id").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("language").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("link").setType(SearchParamType.REFERENCE);
    patient.addSearchParam().setName("name").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("organization").setType(SearchParamType.REFERENCE);
    patient.addSearchParam().setName("phone").setType(SearchParamType.TOKEN);
    patient.addSearchParam().setName("phonetic").setType(SearchParamType.STRING);
    patient.addSearchParam().setName("telecom").setType(SearchParamType.TOKEN);
    patient.addSearchRevInclude(SEARCH_REV_INCLUDE);
    patient.addInteraction().setCode(TypeRestfulInteraction.READ);
    patient.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    patient.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(patient);

    CapabilityStatementRestResourceComponent practitioner =
        new CapabilityStatementRestResourceComponent();
    practitioner.setType(PRACTITIONER);
    practitioner.setProfile(getProfile(PRACTITIONER));
    practitioner.addSupportedProfile(getProfile("us-core-practitioner"));
    practitioner.addSearchParam().setName("active").setType(SearchParamType.TOKEN);
    practitioner.addSearchParam().setName("address").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("address-city").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("address-country").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("address-postalcode").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("address-state").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("address-use").setType(SearchParamType.TOKEN);
    practitioner.addSearchParam().setName("communication").setType(SearchParamType.TOKEN);
    practitioner.addSearchParam().setName("email").setType(SearchParamType.TOKEN);
    practitioner.addSearchParam().setName("family").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("gender").setType(SearchParamType.TOKEN);
    practitioner.addSearchParam().setName("given").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    practitioner.addSearchParam().setName("name").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("phone").setType(SearchParamType.TOKEN);
    practitioner.addSearchParam().setName("phonetic").setType(SearchParamType.STRING);
    practitioner.addSearchParam().setName("telecom").setType(SearchParamType.TOKEN);
    practitioner.addSearchRevInclude(SEARCH_REV_INCLUDE);
    practitioner.addInteraction().setCode(TypeRestfulInteraction.READ);
    practitioner.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    practitioner.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(practitioner);

    CapabilityStatementRestResourceComponent practitionerRole =
        new CapabilityStatementRestResourceComponent();
    practitionerRole.setType(PRACTITIONER_ROLE);
    practitionerRole.setProfile(getProfile(PRACTITIONER_ROLE));
    practitionerRole.addSupportedProfile(getProfile("us-core-practitionerrole"));
    practitionerRole.addSearchParam().setName("active").setType(SearchParamType.TOKEN);
    practitionerRole.addSearchParam().setName("date").setType(SearchParamType.DATE);
    practitionerRole.addSearchParam().setName("email").setType(SearchParamType.TOKEN);
    practitionerRole.addSearchParam().setName("endpoint").setType(SearchParamType.REFERENCE);
    practitionerRole.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    practitionerRole.addSearchParam().setName("location").setType(SearchParamType.REFERENCE);
    practitionerRole.addSearchParam().setName("organization").setType(SearchParamType.REFERENCE);
    practitionerRole.addSearchParam().setName("phone").setType(SearchParamType.TOKEN);
    practitionerRole.addSearchParam().setName("practitioner").setType(SearchParamType.REFERENCE);
    practitionerRole.addSearchParam().setName("role").setType(SearchParamType.TOKEN);
    practitionerRole.addSearchParam().setName("service").setType(SearchParamType.REFERENCE);
    practitionerRole.addSearchParam().setName("specialty").setType(SearchParamType.TOKEN);
    practitionerRole.addSearchParam().setName("telecom").setType(SearchParamType.TOKEN);
    practitionerRole.addSearchRevInclude(SEARCH_REV_INCLUDE);
    practitionerRole.addInteraction().setCode(TypeRestfulInteraction.READ);
    practitionerRole.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    practitionerRole.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(practitionerRole);

    CapabilityStatementRestResourceComponent procedure =
        new CapabilityStatementRestResourceComponent();
    procedure.setType(PROCEDURE);
    procedure.setProfile(getProfile(PROCEDURE));
    procedure.addSupportedProfile(getProfile("us-core-procedure"));
    procedure.addSearchParam().setName("based-on").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("category").setType(SearchParamType.TOKEN);
    procedure.addSearchParam().setName("code").setType(SearchParamType.TOKEN);
    procedure.addSearchParam().setName("date").setType(SearchParamType.DATE);
    procedure.addSearchParam().setName("encounter").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("identifier").setType(SearchParamType.TOKEN);
    procedure.addSearchParam().setName("instantiates-canonical").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("instantiates-uri").setType(SearchParamType.URI);
    procedure.addSearchParam().setName("location").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("part-of").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("patient").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("performer").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("reason-code").setType(SearchParamType.TOKEN);
    procedure.addSearchParam().setName("reason-reference").setType(SearchParamType.REFERENCE);
    procedure.addSearchParam().setName("status").setType(SearchParamType.TOKEN);
    procedure.addSearchParam().setName("subject").setType(SearchParamType.REFERENCE);
    procedure.addSearchRevInclude(SEARCH_REV_INCLUDE);
    procedure.addInteraction().setCode(TypeRestfulInteraction.READ);
    procedure.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    procedure.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
    resources.add(procedure);

    CapabilityStatementRestResourceComponent provenance =
        new CapabilityStatementRestResourceComponent();
    provenance.setType(PROVENANCE);
    provenance.setProfile(getProfile(PROVENANCE));
    provenance.addSupportedProfile(getProfile("us-core-provenance"));
    provenance.addSearchRevInclude(SEARCH_REV_INCLUDE);
    provenance.addInteraction().setCode(TypeRestfulInteraction.READ);
    provenance.addInteraction().setCode(TypeRestfulInteraction.VREAD);
    provenance.addInteraction().setCode(TypeRestfulInteraction.SEARCHTYPE);
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
    implementation.setDescription("HAPI FHIR R4 Server");
    implementation.setUrl(FhirReferenceServerUtils.getServerBaseUrl(theRequest));
    capabilityStatement.setImplementation(implementation);

    capabilityStatement.setFhirVersion(FHIRVersion._4_0_1);

    capabilityStatement.addFormat("application/fhir+xml");
    capabilityStatement.addFormat("application/fhir+json");

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
