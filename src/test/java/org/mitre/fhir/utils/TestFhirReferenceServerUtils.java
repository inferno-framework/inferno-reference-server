package org.mitre.fhir.utils;

import java.util.ArrayList;
import java.util.List;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class TestFhirReferenceServerUtils {

  private static final MockHttpServletRequest mockRequest = new MockHttpServletRequest();

  @Test
  public void testGetServerBaseUrl() {
    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockRequest);
    Assert.assertEquals("http://www.example.org:123/reference-server", baseUrl);
  }

  @Test
  public void testGetServerBaseUrlWithHttpDefaultPort() {
    mockRequest.setServerPort(80);
    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockRequest);
    Assert.assertEquals("http://www.example.org/reference-server", baseUrl);
  }

  @Test
  public void testGetServerBaseUrlWithHttpsDefaultPort() {
    mockRequest.setScheme("https");
    mockRequest.setServerPort(443);
    String baseUrl = FhirReferenceServerUtils.getServerBaseUrl(mockRequest);
    Assert.assertEquals("https://www.example.org/reference-server", baseUrl);
  }

  @Test
  public void testGetFhirServerBaseUrl() {
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
    IGenericClient newClient = FhirReferenceServerUtils.getClientFromRequest(mockRequest);
    Assert.assertEquals(newClient.getServerBase(), "http://www.example.org/reference-server/r4");

    IGenericClient existingClient = FhirReferenceServerUtils.getClientFromRequest(mockRequest);
    Assert.assertEquals(existingClient, newClient);
  }

  @Test
  public void testGetClientFromRequestCreatesNewClient() {
    IGenericClient client = FhirReferenceServerUtils.getClientFromRequest(mockRequest);
    Assert.assertEquals(client.getServerBase(), "http://www.example.org/reference-server/r4");

    MockHttpServletRequest differentMockRequest = new MockHttpServletRequest();
    differentMockRequest.setScheme("https");
    differentMockRequest.setServerName("www.notexample.org");

    IGenericClient differentClient = FhirReferenceServerUtils.getClientFromRequest(differentMockRequest);
    Assert.assertEquals(differentClient.getServerBase(), "https://www.notexample.org:80/reference-server/r4");
    Assert.assertNotEquals(client, differentClient);
  }

  @BeforeClass
  public static void beforeClass() throws Exception {
    mockRequest.setScheme("http");
    mockRequest.setServerName("www.example.org");
    mockRequest.setRequestURI("/.well-known/smart-configuration");
  }
}
