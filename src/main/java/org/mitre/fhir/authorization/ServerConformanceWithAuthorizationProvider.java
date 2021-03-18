package org.mitre.fhir.authorization;

import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamRegistry;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceOperationComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
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


  private static final String LOCATION_RESOURCE_TYPE = "Location";
  private static final String NEAR_SEARCH_PARAM_NAME = "near";

  private static final String SEARCH_REV_INCLUDE = "Provenance:target";

  private static final Map<String, String[]> usCoreProfiles = new HashMap<>();

  static {
    usCoreProfiles.put("AllergyIntolerance", new String[] {
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-allergyintolerance"});
    usCoreProfiles.put("CarePlan",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-careplan"});
    usCoreProfiles.put("CareTeam",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-careteam"});
    usCoreProfiles.put("Condition",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-condition"});
    usCoreProfiles.put("Device", new String[] {
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-implantable-device"});
    usCoreProfiles.put("DiagnosticReport",
        new String[] {
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-lab",
            "http://hl7.org/fhir/us/core/StructureDefinition/us-core-diagnosticreport-note"});
    usCoreProfiles.put("DocumentReference",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-documentreference"});
    usCoreProfiles.put("Encounter",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-encounter"});
    usCoreProfiles.put("Goal",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-goal"});
    usCoreProfiles.put("Immunization",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-immunization"});
    usCoreProfiles.put("Location",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-location"});
    usCoreProfiles.put("Medication",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-medication"});
    usCoreProfiles.put("MedicationRequest",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-medicationrequest"});
    usCoreProfiles.put("Observation", new String[] {
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab",
        "http://hl7.org/fhir/us/core/StructureDefinition/pediatric-bmi-for-age",
        "http://hl7.org/fhir/us/core/StructureDefinition/pediatric-weight-for-height",
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-pulse-oximetry",
        "http://hl7.org/fhir/us/core/StructureDefinition/us-core-smokingstatus",
        "http://hl7.org/fhir/StructureDefinition/vitalsigns",
        "http://hl7.org/fhir/StructureDefinition/bodyheight",
        "http://hl7.org/fhir/StructureDefinition/heartrate",
        "http://hl7.org/fhir/StructureDefinition/bodyweight",
        "http://hl7.org/fhir/us/core/StructureDefinition/head-occipital-frontal-circumference-percentile",
        "http://hl7.org/fhir/StructureDefinition/resprate",
        "http://hl7.org/fhir/StructureDefinition/bodytemp",
        "http://hl7.org/fhir/StructureDefinition/bp"});
    usCoreProfiles.put("Organization",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-organization"});
    usCoreProfiles.put("Patient",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-patient"});
    usCoreProfiles.put("Practitioner",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitioner"});
    usCoreProfiles.put("PractitionerRole",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-practitionerrole"});
    usCoreProfiles.put("Procedure",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-procedure"});
    usCoreProfiles.put("Provenance",
        new String[] {"http://hl7.org/fhir/us/core/StructureDefinition/us-core-provenance"});
  }



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

  private void fixListResource(CapabilityStatementRestComponent restComponents) {
    restComponents.getResource().stream()
        .filter(restResource -> "List".equals(restResource.getType())).findFirst()
        .ifPresent(listResource -> listResource
            .setProfile("http://hl7.org/fhir/StructureDefinition/List"));
  }

  @Override
  public CapabilityStatement getServerConformance(HttpServletRequest theRequest,
      RequestDetails theRequestDetails) {
    Extension oauthUris = new Extension();
    oauthUris.setUrl(OAUTH_URL); // url

    oauthUris.addExtension(
        new Extension(TOKEN_EXTENSION_URL, new UriType(getTokenExtensionUri(theRequest))));

    oauthUris.addExtension(new Extension(AUTHORIZE_EXTENSION_URL,
        new UriType(getAuthorizationExtensionUri(theRequest))));

    oauthUris.addExtension(
        new Extension(REVOKE_EXTENSION_URL, new UriType(getRevokeExtensionUri(theRequest))));

    CapabilityStatementRestSecurityComponent security =
        new CapabilityStatementRestSecurityComponent();
    security.addExtension(oauthUris);

    CodeableConcept service = security.addService();
    Coding coding = service.addCoding();
    coding.setSystem("http://hl7.org/fhir/ValueSet/restful-security-service");
    coding.setCode("SMART-on-FHIR");

    service.setText("OAuth2 using SMART-on-FHIR profile (see http://docs.smarthealthit.org)");


    CapabilityStatement capabilityStatement =
        super.getServerConformance(theRequest, theRequestDetails);
    CapabilityStatementRestComponent rest = capabilityStatement.getRest().get(0);

    // Add supported US Core Profiles
    List<CapabilityStatementRestResourceComponent> resources = rest.getResource();
    for (CapabilityStatementRestResourceComponent component : resources) {
      String resourceType = component.getType();
      String[] supportedProfiles = usCoreProfiles.get(resourceType);
      if (supportedProfiles != null) {
        for (String supportedProfile : supportedProfiles) {
          component.addSupportedProfile(supportedProfile);
        }
      }
    }

    rest.setSecurity(security);

    fixListResource(rest);

    rest.setOperation(new ArrayList<CapabilityStatementRestResourceOperationComponent>());

    // Location searchParam "near" is missing type, need to add it
    // https://www.hl7.org/fhir/location.html
    for (CapabilityStatementRestResourceComponent capabilityStatementRestResourceComponent : rest
        .getResource()) {
      capabilityStatementRestResourceComponent.addSearchRevInclude(SEARCH_REV_INCLUDE);

      if (LOCATION_RESOURCE_TYPE.equals(capabilityStatementRestResourceComponent.getType())) {
        List<CapabilityStatementRestResourceSearchParamComponent> searchParams =
            capabilityStatementRestResourceComponent.getSearchParam();
        for (CapabilityStatementRestResourceSearchParamComponent searchParam : searchParams) {
          if (NEAR_SEARCH_PARAM_NAME.equals(searchParam.getName())) {
            searchParam.setType(SearchParamType.SPECIAL);
          }
        }
      }
    }

    return capabilityStatement;
  }
}
