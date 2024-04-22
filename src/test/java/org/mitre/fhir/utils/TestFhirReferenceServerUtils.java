package org.mitre.fhir.utils;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestFhirReferenceServerUtils {

  @Test
  public void testGetServerBaseUrl() {
    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(getMockRequest());
    Assert.assertEquals("http://www.example.org/reference-server", baseUrl);
  }

  @Test
  public void testGetServerBaseUrlWithHttpDefaultPort() {
    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(getMockRequest());
    Assert.assertEquals("http://www.example.org/reference-server", baseUrl);
  }

  @Test
  public void testGetServerBaseUrlWithHttpsDefaultPort() {
    MockHttpServletRequest mockRequest = getMockRequest();
    mockRequest.setScheme("https");
    mockRequest.setServerPort(443);
    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockRequest);
    Assert.assertEquals("https://www.example.org/reference-server", baseUrl);
  }

  @Test
  public void testGetFhirServerBaseUrl() {
    MockHttpServletRequest mockRequest = getMockRequest();
    mockRequest.setServerPort(123);
    String baseUrl = FhirReferenceServerUtils.getFhirServerBaseUrl(mockRequest);
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
  public void testGetScopesListByScopesString3() {
    String scopesString =
        " launch launch/patient    offline_access openid profile  user/*.* patient/*.*   ";
    List<String> scopesList = FhirReferenceServerUtils.getScopesListByScopeString(scopesString);
    Assert.assertEquals(scopesList.size(), 7);

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
    List<String> scopesList = new ArrayList<>();
    scopesList.add("scope1");
    scopesList.add("scope2");
    scopesList.add("scope3");

    String scopesString = FhirReferenceServerUtils.getScopesStringFromScopesList(scopesList);

    Assert.assertEquals("scope1 scope2 scope3", scopesString);
  }

  @Test
  public void testGetScopesStringFromScopesListEmptyList() {
    List<String> scopesList = new ArrayList<>();

    String scopesString = FhirReferenceServerUtils.getScopesStringFromScopesList(scopesList);

    Assert.assertEquals("", scopesString);

  }

  @Test
  public void testGetScopesStringFromScopesListNullList() {
    String scopesString = FhirReferenceServerUtils.getScopesStringFromScopesList(null);

    Assert.assertEquals("", scopesString);

  }

  @Test
  public void testGetClientFromRequestReturnsExistingClient() {
    MockHttpServletRequest mockRequest = getMockRequest();

    IGenericClient newClient = FhirReferenceServerUtils.getClientFromRequest(mockRequest);
    Assert.assertEquals(newClient.getServerBase(), "http://www.example.org/reference-server/r4");

    IGenericClient existingClient = FhirReferenceServerUtils.getClientFromRequest(getMockRequest());
    Assert.assertEquals(existingClient, newClient);
  }

  @Test
  public void testGetClientFromRequestCreatesNewClient() {
    IGenericClient client = FhirReferenceServerUtils.getClientFromRequest(getMockRequest());
    Assert.assertEquals(client.getServerBase(), "http://www.example.org/reference-server/r4");

    MockHttpServletRequest newMockRequest = new MockHttpServletRequest();
    newMockRequest.setScheme("https");
    newMockRequest.setServerName("www.notexample.org");

    IGenericClient differentClient = FhirReferenceServerUtils.getClientFromRequest(newMockRequest);
    Assert.assertEquals(differentClient.getServerBase(),
        "https://www.notexample.org:80/reference-server/r4");
    Assert.assertNotEquals(client, differentClient);
  }

  private static MockHttpServletRequest getMockRequest() {
    MockHttpServletRequest mockRequest = new MockHttpServletRequest();

    mockRequest.setScheme("http");
    mockRequest.setServerPort(80);
    mockRequest.setServerName("www.example.org");
    mockRequest.setRequestURI("/.well-known/smart-configuration");

    return mockRequest;
  }
}
