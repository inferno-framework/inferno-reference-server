package gov.onc.authorization;

import javax.servlet.http.HttpServletRequest;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestComponent;
import org.hl7.fhir.r4.model.CapabilityStatement.CapabilityStatementRestSecurityComponent;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.UriType;

import com.github.dnault.xmlpatch.internal.Log;

import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.r4.JpaConformanceProviderR4;
import ca.uhn.fhir.rest.server.RestfulServer;



public class ServerConformanceWithAuthorizationProvider extends JpaConformanceProviderR4 {
	
	private static final String OAUTH_URL = "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris";
	
	public ServerConformanceWithAuthorizationProvider(RestfulServer theRestfulServer, IFhirSystemDao<Bundle, Meta> theSystemDao, DaoConfig theDaoConfig) {
		super(theRestfulServer, theSystemDao, theDaoConfig);
		setCache(false); //set cache to false to prevent caching and readding elements in getServerConformance
	}
	
	
	
	@Override
	public CapabilityStatement getServerConformance(HttpServletRequest theRequest) {
		CapabilityStatement capabilityStatement = super.getServerConformance(theRequest);
				
		Log.info("numm of rests are " + capabilityStatement.getRest().size());
		CapabilityStatementRestComponent rest = capabilityStatement.getRest().get(0);
		
		CapabilityStatementRestSecurityComponent security = new CapabilityStatementRestSecurityComponent();

		Extension oauthUris = new Extension();
		oauthUris.setUrl(OAUTH_URL); //url
		
		Extension tokenExtension = new Extension();
		tokenExtension.setUrl("token");
		UriType value = new UriType();
		value.setValue("https://example.com/token");
		//tokenExtension.addChild("valueURI");//, new Extension().setValue(value));		
		
		//tokenExtension.
		
		tokenExtension.setValue(value);//valueUri
		oauthUris.addExtension(tokenExtension);
		security.addExtension(oauthUris);		
		rest.setSecurity(security);
		
		

		Log.info("Logging getServerConf!!!!!!!!!");

		//capabilityStatementRestComponent.addChild("security");
		//capabilityStatementRestComponent.
		//capabilityStatement.getRest().add(capabilityStatementRestComponent);
		
		return capabilityStatement;
		
	}

}
