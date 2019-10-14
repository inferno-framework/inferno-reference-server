package org.mitre.fhir.authorization;

import java.io.IOException;

import org.springframework.mock.web.MockHttpServletRequest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Assert;
import org.junit.Test;
import org.mitre.fhir.wellknown.WellKnownAuthorizationEndpointController;

public class TestWellKnownEndpoint {

	@Test
	public void testWellKnownEndpoint() throws IOException {
		WellKnownAuthorizationEndpointController wellKnownEndpoint = new WellKnownAuthorizationEndpointController();
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

		mockHttpServletRequest.setScheme("http");
		mockHttpServletRequest.setServerName("www.example.org");
		mockHttpServletRequest.setServerPort(123);
		mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");

		String jSONString = wellKnownEndpoint.getWellKnownJSON(mockHttpServletRequest);

		ObjectMapper mapper = new ObjectMapper();
		JsonNode jsonNode = mapper.readTree(jSONString);

		String authorizationEndpoint = jsonNode.get("authorization_endpoint").asText();
		Assert.assertEquals("http://www.example.org:123/oauth/authorization", authorizationEndpoint);

		String tokenEndpoint = jsonNode.get("token_endpoint").asText();
		Assert.assertEquals("http://www.example.org:123/oauth/token", tokenEndpoint);
	}
}
