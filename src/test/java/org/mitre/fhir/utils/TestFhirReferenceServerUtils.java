package org.mitre.fhir.utils;

import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestFhirReferenceServerUtils {

  @Test
  public void testGetServerBaseUrl() {
    MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    mockHttpServletRequest.setScheme("http");
    mockHttpServletRequest.setServerName("www.example.org");
    mockHttpServletRequest.setServerPort(123);
    mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");

    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockHttpServletRequest);
    Assert.assertEquals("http://www.example.org:123/reference-server", baseUrl);

  }

  @Test
  public void testGetServerBaseUrlWithEnvVar() throws Exception {
    SystemLambda.withEnvironmentVariable("CUSTOM_PORT", "8443").execute(() -> {
      MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

      mockHttpServletRequest.setScheme("http");
      mockHttpServletRequest.setServerName("www.example.org");
      mockHttpServletRequest.setServerPort(8443);
      mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");

      String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockHttpServletRequest);
      Assert.assertEquals("http://www.example.org/reference-server", baseUrl);

    });

  }
  
  @Test
  public void testGetServerBaseUrlWithEnvVarInvalidValue() throws Exception {
    SystemLambda.withEnvironmentVariable("CUSTOM_PORT", "TEST").execute(() -> {
      MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

      mockHttpServletRequest.setScheme("http");
      mockHttpServletRequest.setServerName("www.example.org");
      mockHttpServletRequest.setServerPort(8443);
      mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");

      String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockHttpServletRequest);
      Assert.assertEquals("http://www.example.org:8443/reference-server", baseUrl);

    });

  }

  @Test
  public void testGetServerBaseUrlWithHttpDefaultPort() {
    MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    mockHttpServletRequest.setScheme("http");
    mockHttpServletRequest.setServerName("www.example.org");
    mockHttpServletRequest.setServerPort(80);
    mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");

    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockHttpServletRequest);
    Assert.assertEquals("http://www.example.org/reference-server", baseUrl);
  }

  @Test
  public void testGetServerBaseUrlWithHttpsDefaultPort() {
    MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    mockHttpServletRequest.setScheme("https");
    mockHttpServletRequest.setServerName("www.example.org");
    mockHttpServletRequest.setServerPort(443);
    mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");

    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockHttpServletRequest);
    Assert.assertEquals("https://www.example.org/reference-server", baseUrl);
  }

  @Test
  public void testGetFhirServerBaseUrl() {
    MockHttpServletRequest mockHttpServletRequest = new MockHttpServletRequest();

    mockHttpServletRequest.setScheme("http");
    mockHttpServletRequest.setServerName("www.example.org");
    mockHttpServletRequest.setServerPort(123);
    mockHttpServletRequest.setRequestURI("/.well-known/smart-configuration");

    String baseUrl = FhirReferenceServerUtils.getFhirServerBaseUrl(mockHttpServletRequest);
    Assert.assertEquals("http://www.example.org:123/reference-server/r4", baseUrl);
  }

}
