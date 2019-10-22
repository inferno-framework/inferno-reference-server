package org.mitre.fhir.authorization;

import org.junit.Assert;
import org.junit.Test;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.springframework.mock.web.MockHttpServletRequest;


public class TestFhirReferenceServerUtils {
	
	@Test
	public void testGetServerBaseUrl()
	{
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

		mockHttpServletRequest.setScheme("http");
		mockHttpServletRequest.setServerName("www.example.org");
		mockHttpServletRequest.setServerPort(123);
		mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");
		
		String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockHttpServletRequest);
		Assert.assertEquals("http://www.example.org:123", baseUrl);
	}
	
	@Test
	public void testGetFhirServerBaseUrl()
	{
		MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

		mockHttpServletRequest.setScheme("http");
		mockHttpServletRequest.setServerName("www.example.org");
		mockHttpServletRequest.setServerPort(123);
		mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");
		
		String baseUrl = FhirReferenceServerUtils.getFhirServerBaseUrl(mockHttpServletRequest);
		Assert.assertEquals("http://www.example.org:123/r4", baseUrl);
	}

}
