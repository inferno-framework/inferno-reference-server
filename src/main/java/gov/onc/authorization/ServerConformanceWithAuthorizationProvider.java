package gov.onc.authorization;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.UriType;

import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.rest.server.RestfulServer;

public class ServerConformanceWithAuthorizationProvider extends JpaConformanceProviderR4 {
	
	private static final String OAUTH_URL = "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";

	private static final String TOKEN_EXTENSION_URL = "token";
	private static final String TOKEN_EXTENSION_VALUE_URI = "http://localhost:8080/hapi-fhir-jpaserver/oauth/token"; //this needs to relative

	private static final String AUTHORIZE_EXTENSION_URL = "authorize";
	private static final String AUTHORIZE_EXTENSION_VALUE_URI = "http://localhost:8080/hapi-fhir-jpaserver/oauth/authorization"; //this needs to relative

	public ServerConformanceWithAuthorizationProvider(RestfulServer theRestfulServer, IFhirSystemDao<Bundle, Meta> theSystemDao, DaoConfig theDaoConfig) {
		super(theRestfulServer, theSystemDao, theDaoConfig);
		setCache(false); //set cache to false to prevent caching and readding elements in getServerConformance
	}
	
	@Override
	public CapabilityStatement getServerConformance(HttpServletRequest theRequest) {
		CapabilityStatement capabilityStatement = super.getServerConformance(theRequest);
				
		CapabilityStatementRestComponent rest = capabilityStatement.getRest().get(0);
		
		CapabilityStatementRestSecurityComponent security = new CapabilityStatementRestSecurityComponent();

		Extension oauthUris = new Extension();
		oauthUris.setUrl(OAUTH_URL); //url
		
		Extension tokenExtension = new Extension();
		tokenExtension.setUrl(TOKEN_EXTENSION_URL);
		UriType tokenValue = new UriType();
		tokenValue.setValue(TOKEN_EXTENSION_VALUE_URI);		
		tokenExtension.setValue(tokenValue);//valueUri
		oauthUris.addExtension(tokenExtension);

		Extension authorizeExtension = new Extension();
		authorizeExtension.setUrl(AUTHORIZE_EXTENSION_URL);
		UriType authorizeValue = new UriType();
		authorizeValue.setValue(AUTHORIZE_EXTENSION_VALUE_URI);		
		authorizeExtension.setValue(authorizeValue);//valueUri
		oauthUris.addExtension(authorizeExtension);
		
		security.addExtension(oauthUris);		
		rest.setSecurity(security);
		
		return capabilityStatement;		
	}

}
