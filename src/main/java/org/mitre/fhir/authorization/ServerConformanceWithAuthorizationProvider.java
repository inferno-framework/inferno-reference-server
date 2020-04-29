package org.mitre.fhir.authorization;

import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.RestfulServer;
import javax.servlet.http.HttpServletRequest;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestResourceSearchParamComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.mitre.fhir.utils.FhirReferenceServerUtils;

public class ServerConformanceWithAuthorizationProvider extends JpaConformanceProviderR4 {

  public static final String TOKEN_EXTENSION_URL = "token";
  public static final String AUTHORIZE_EXTENSION_URL = "authorize";
  private static final String OAUTH_URL = "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";
  private static final String TOKEN_EXTENSION_VALUE_URI = "/oauth/token";
  private static final String AUTHORIZE_EXTENSION_VALUE_URI = "/oauth/authorization";

  private static final String LOCATION_RESOURCE_TYPE = "Location";
  private static final String NEAR_SEARCH_PARAM_NAME = "near";

  private static final String SEARCH_REV_INCLUDE = "Provenance:target";


  public ServerConformanceWithAuthorizationProvider(RestfulServer theRestfulServer,
                                                    IFhirSystemDao<Bundle, Meta> theSystemDao, DaoConfig theDaoConfig) {
    super(theRestfulServer, theSystemDao, theDaoConfig);
  }

  public static String getTokenExtensionURI(HttpServletRequest theRequest) {
    return FhirReferenceServerUtils.getServerBaseUrl(theRequest) + TOKEN_EXTENSION_VALUE_URI;
  }

  public static String getAuthorizationExtensionURI(HttpServletRequest theRequest) {
    return FhirReferenceServerUtils.getServerBaseUrl(theRequest) + AUTHORIZE_EXTENSION_VALUE_URI;
  }

  private void fixListResource(CapabilityStatementRestComponent restComponents) {
    restComponents
        .getResource()
        .stream()
        .filter(restResource -> "List".equals(restResource.getType()))
        .findFirst()
        .ifPresent(listResource -> listResource.setProfile("http://hl7.org/fhir/StructureDefinition/List"));
  }

  @Override
  public CapabilityStatement getServerConformance(HttpServletRequest theRequest, RequestDetails theRequestDetails) {
    CapabilityStatement capabilityStatement = super.getServerConformance(theRequest, theRequestDetails);

    CapabilityStatementRestComponent rest = capabilityStatement.getRest().get(0);

    CapabilityStatementRestSecurityComponent security = new CapabilityStatementRestSecurityComponent();

    Extension oauthUris = new Extension();
    oauthUris.setUrl(OAUTH_URL); //url

    Extension tokenExtension = new Extension();
    tokenExtension.setUrl(TOKEN_EXTENSION_URL);
    UriType tokenValue = new UriType();
    tokenValue.setValue(getTokenExtensionURI(theRequest));
    tokenExtension.setValue(tokenValue);//valueUri
    oauthUris.addExtension(tokenExtension);

    Extension authorizeExtension = new Extension();
    authorizeExtension.setUrl(AUTHORIZE_EXTENSION_URL);
    UriType authorizeValue = new UriType();
    authorizeValue.setValue(getAuthorizationExtensionURI(theRequest));
    authorizeExtension.setValue(authorizeValue);//valueUri
    oauthUris.addExtension(authorizeExtension);

    security.addExtension(oauthUris);
    rest.setSecurity(security);

    fixListResource(rest);

    //Location searchParam "near" is missing type, need to add it
    //https://www.hl7.org/fhir/location.html
    for (CapabilityStatementRestResourceComponent capabilityStatementRestResourceComponent : rest.getResource()) {
      capabilityStatementRestResourceComponent.addSearchRevInclude(SEARCH_REV_INCLUDE);

      if (LOCATION_RESOURCE_TYPE.equals(capabilityStatementRestResourceComponent.getType())) {
        for (CapabilityStatementRestResourceSearchParamComponent searchParam : capabilityStatementRestResourceComponent.getSearchParam()) {
          if (NEAR_SEARCH_PARAM_NAME.equals(searchParam.getName())) {
            searchParam.setType(SearchParamType.SPECIAL);
          }
        }
      }
    }

    return capabilityStatement;
  }
}
