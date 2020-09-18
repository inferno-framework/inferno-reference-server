package org.mitre.fhir.utils;

import java.util.ArrayList;
import java.util.List;
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

  @Test
  public void testGetScopesListByScopesString() {
    String scopesString = " scope1 scope2  scope3        scope4  ";
    List<String> scopesList = FhirReferenceServerUtils.getScopesListByScopeString(scopesString);

    Assert.assertEquals(scopesList.size(), 4);
  }

  @Test
  public void testGetScopesListByScopesString2() {
    String scopesString = "scope1 scope2  scope3        scope4";
    List<String> scopesList = FhirReferenceServerUtils.getScopesListByScopeString(scopesString);

    Assert.assertEquals(scopesList.size(), 4);
  }

  @Test
  public void testGetScopesListByScopesStringWithNull() {
    String scopesString = null;
    List<String> scopesList = FhirReferenceServerUtils.getScopesListByScopeString(scopesString);

    Assert.assertEquals(scopesList.size(), 0);
  }

  @Test
  public void testGetScopesListByScopesStringWithEmptyString() {
    String scopesString = "";
    List<String> scopesList = FhirReferenceServerUtils.getScopesListByScopeString(scopesString);

    Assert.assertEquals(scopesList.size(), 0);
  }

  @Test
  public void testGetScopesStringFromScopesList() {
    List<String> scopesList = new ArrayList<String>();
    scopesList.add("scope1");
    scopesList.add("scope2");
    scopesList.add("scope3");

    String scopesString = FhirReferenceServerUtils.getScopesStringFromScopesList(scopesList);

    Assert.assertEquals("scope1 scope2 scope3", scopesString);
  }

  @Test
  public void testGetScopesStringFromScopesListEmptyList() {
    List<String> scopesList = new ArrayList<String>();

    String scopesString = FhirReferenceServerUtils.getScopesStringFromScopesList(scopesList);

    Assert.assertEquals("", scopesString);

  }

  @Test
  public void testGetScopesStringFromScopesListNullList() {
    String scopesString = FhirReferenceServerUtils.getScopesStringFromScopesList(null);

    Assert.assertEquals("", scopesString);

  }
}
