package gov.onc.authorization;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.starter.ExampleServerR4IT;
import ca.uhn.fhir.jpa.starter.HapiProperties;
import ca.uhn.fhir.jpa.starter.RandomServerPortProvider;
import ca.uhn.fhir.jpa.starter.SocketImplementation;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.util.PortUtil;
import gov.onc.authorization.TestUtils;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Subscription;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static ca.uhn.fhir.util.TestUtil.waitForSize;
import static org.junit.Assert.assertEquals;

public class TestAuthorization {

    private static final org.slf4j.Logger ourLog = org.slf4j.LoggerFactory.getLogger(ExampleServerR4IT.class);
    private static IGenericClient ourClient;
    private static FhirContext ourCtx;
    private static int ourPort;

    private static Server ourServer;
    private static String ourServerBase;

    static {
        HapiProperties.forceReload();
        HapiProperties.setProperty(HapiProperties.DATASOURCE_URL, "jdbc:derby:memory:dbr4;create=true");
        HapiProperties.setProperty(HapiProperties.FHIR_VERSION, "R4");
        HapiProperties.setProperty(HapiProperties.SUBSCRIPTION_WEBSOCKET_ENABLED, "true");
        ourCtx = FhirContext.forR4();
        ourPort = PortUtil.findFreePort();
    }

    @Test
    public void testCreateAndRead() {
        ourLog.info("Base URL is: " +  HapiProperties.getServerAddress());
        String methodName = "testCreateResourceConditional";

        Patient pt = new Patient();
        pt.addName().setFamily(methodName);
        IIdType id = ourClient.create().resource(pt).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE).execute().getId();

        Patient pt2 = ourClient.read().resource(Patient.class).withId(id).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE).execute();
        assertEquals(methodName, pt2.getName().get(0).getFamily());
    }
    
    @Test
    public void testInterceptor()
    {
        String methodName = "testCreateResourceConditional";
        
        //with no header
        Patient pt = new Patient();
        pt.addName().setFamily(methodName);
        
        try
        {
        	ourClient.create().resource(pt).execute();
            Assert.fail();
        }
        
        catch (AuthenticationException ae)
        {
        	
        }
        
    }
    
    @Test
    public void testTestAuthorizationWithInvalidCode()
    {
    	AuthorizationController authorizationController = new AuthorizationController();
    	
    	try {
    		authorizationController.getToken("INVALID_CODE");
        	Assert.fail();
    	}
    	
    	catch (ResponseStatusException rse)
    	{
    		if (!HttpStatus.UNAUTHORIZED.equals(rse.getStatus()))
    		{ 		
    			Assert.fail();
    		}
    	}
    }
    
    //replace these with actual post requests
    @Test
    public void testTestAuthorizationWithValidCode() throws IOException
    {
    	AuthorizationController authorizationController = new AuthorizationController();
    	
    	try {
    		ResponseEntity<String> tokenResponseEntity = authorizationController.getToken("SAMPLE_CODE");

    		ObjectMapper mapper = new ObjectMapper();
    		
    		String jSONString = tokenResponseEntity.getBody();
    	
    		JsonNode jsonNode = mapper.readTree(jSONString);
    		String accessToken = jsonNode.get("access_token").asText();
    		
    		Assert.assertEquals("SAMPLE_ACCESS_TOKEN", accessToken);
    	}
    	
    	catch (ResponseStatusException rse)
    	{
    		Assert.fail();
    	}
    }
    
    @Test
    public void testCapabilityStatementNotBlockedByInterceptor()
    {
    	//should throw an exception if interceptor
    	try
    	{
    		ourClient.capabilities().ofType(CapabilityStatement.class).execute();
    	}
    	
    	catch (AuthenticationException ae)
    	{
    		Assert.fail();
    	}
    }
    
    
    @Test
    public void testWebsocketSubscription() throws Exception {
        /*
         * Create subscription
         */
        Subscription subscription = new Subscription();
        subscription.setReason("Monitor new neonatal function (note, age will be determined by the monitor)");
        subscription.setStatus(Subscription.SubscriptionStatus.REQUESTED);
        subscription.setCriteria("Observation?status=final");

        Subscription.SubscriptionChannelComponent channel = new Subscription.SubscriptionChannelComponent();
        channel.setType(Subscription.SubscriptionChannelType.WEBSOCKET);
        channel.setPayload("application/json");
        subscription.setChannel(channel);

        MethodOutcome methodOutcome = ourClient.create().resource(subscription).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE).execute();
        IIdType mySubscriptionId = methodOutcome.getId();

        // Wait for the subscription to be activated
        waitForSize(1, () -> ourClient.search().forResource(Subscription.class).where(Subscription.STATUS.exactly().code("active")).cacheControl(new CacheControlDirective().setNoCache(true)).returnBundle(Bundle.class).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE).execute().getEntry().size());

        /*
         * Attach websocket
         */

        WebSocketClient myWebSocketClient = new WebSocketClient();
        SocketImplementation mySocketImplementation = new SocketImplementation(mySubscriptionId.getIdPart(), EncodingEnum.JSON);

        myWebSocketClient.start();
        URI echoUri = new URI("ws://localhost:" + ourPort + "/hapi-fhir-jpaserver/websocket");
        ClientUpgradeRequest request = new ClientUpgradeRequest();
        ourLog.info("Connecting to : {}", echoUri);
        Future<Session> connection = myWebSocketClient.connect(mySocketImplementation, echoUri, request);
        Session session = connection.get(2, TimeUnit.SECONDS);

        ourLog.info("Connected to WS: {}", session.isOpen());

        /*
         * Create a matching resource
         */
        Observation obs = new Observation();
        obs.setStatus(Observation.ObservationStatus.FINAL);
        ourClient.create().resource(obs).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE).execute();

        // Give some time for the subscription to deliver
        Thread.sleep(2000);

        /*
         * Ensure that we receive a ping on the websocket
         */
        waitForSize(1, () -> mySocketImplementation.getMyPingCount());

        /*
         * Clean up
         */
        ourClient.delete().resourceById(mySubscriptionId).withAdditionalHeader(TestUtils.AUTHORIZATION_HEADER_NAME, TestUtils.AUTHORIZATION_HEADER_VALUE).execute();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ourServer.stop();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        String path = Paths.get("").toAbsolutePath().toString();

        ourLog.info("Project base path is: {}", path);

        if (ourPort == 0) {
            ourPort = RandomServerPortProvider.findFreePort();
        }
        ourServer = new Server(ourPort);

        WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/hapi-fhir-jpaserver");
        webAppContext.setDisplayName("HAPI FHIR");
        webAppContext.setDescriptor(path + "/src/main/webapp/WEB-INF/web.xml");
        webAppContext.setResourceBase(path + "/target/hapi-fhir-jpaserver-starter");
        webAppContext.setParentLoaderPriority(true);

        ourServer.setHandler(webAppContext);
        ourServer.start();

        ourCtx.getRestfulClientFactory().setServerValidationMode(ServerValidationModeEnum.NEVER);
        ourCtx.getRestfulClientFactory().setSocketTimeout(1200 * 1000);
        ourServerBase = HapiProperties.getServerAddress();
        ourServerBase = "http://localhost:" + ourPort + "/hapi-fhir-jpaserver/fhir/";

        ourClient = ourCtx.newRestfulGenericClient(ourServerBase);
        ourClient.registerInterceptor(new LoggingInterceptor(true));
    }

    public static void main(String[] theArgs) throws Exception {
        ourPort = 8080;
        beforeClass();
    }
}
