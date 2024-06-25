package org.mitre.fhir.utils;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;

public class FhirReferenceServerUtils {

  public static final String SAMPLE_CODE = "SAMPLE_CODE";

  public static final String AUTHORIZATION_HEADER_NAME = "Authorization";
  public static final String BEARER_TOKEN_PREFIX = "Bearer";
  public static final String FHIR_SERVER_PATH = "/r4";
  public static final String REFERENCE_SERVER_PATH = "/app";

  public static final String DEFAULT_SCOPE = "launch/patient patient/*.*";

  /**
   * FHIR Context for this server.
   * FhirContext objects are expensive to create, so only create one per-app
   * and reuse it everywhere.
   */
  public static final FhirContext FHIR_CONTEXT_R4 = FhirContext.forR4();

  private static final String HTTP = "http";
  private static final String HTTPS = "https";
  private static final int HTTP_DEFAULT_PORT = 80;
  private static final int HTTPS_DEFAULT_PORT = 443;
  private static final Map<String, IGenericClient> clients = new HashMap<>();

  /**
   * Get the server's base url.
   * 
   * @param request web service request
   * @return string representation of the base url of the input request
   */
  public static String getServerBaseUrl(HttpServletRequest request) {
    String scheme = request.getScheme();
    int portNumber = request.getServerPort();
    String port = ":" + portNumber;

    // if default port, remove the port
    if ((HTTP.equals(scheme) && portNumber == HTTP_DEFAULT_PORT)
        || (HTTPS.equals(scheme) && portNumber == HTTPS_DEFAULT_PORT)) {
      port = "";
    }

    return request.getScheme() + "://" + request.getServerName() + port + "/reference-server";
  }

  public static String getFhirServerBaseUrl(HttpServletRequest request) {
    return getServerBaseUrl(request) + FHIR_SERVER_PATH;
  }

  public static String getSmartStyleUrl(HttpServletRequest request) {
    return getServerBaseUrl(request) + REFERENCE_SERVER_PATH + "/smart-style-url";
  }

  /**
   * Create a code embed with the actual code, scopes and patient id.
   * 
   * @param actualCode the code itself for auth purposes
   * @param scopes the scopes the user selected for this token
   * @param patientId the selected patientId
   * @return string representation of code containing the input code, scopes, and patientId
   */
  public static String createCode(String actualCode, String scopes, String patientId) {
    JSONObject code = new JSONObject();

    if (actualCode != null) {
      code.put("code", actualCode);
    }
    if (scopes != null) {
      code.put("scopes", scopes);
    }
    if (patientId != null) {
      code.put("patientId", patientId);
    }

    return Base64.getEncoder().encodeToString(code.toString().getBytes());
  }

  /**
   * Create a code embed with the actual code, scopes and patient id.
   *
   * @param actualCode the code itself for auth purposes
   * @param scopes the scopes the user selected for this token
   * @param patientId the selected patientId
   * @param codeChallengeMethod PKCE
   * @param codeChallenge PKCE
   * @return string representation of code containing the input code, scopes, and patientId
   */
  public static String createCode(
      String actualCode,
      String scopes,
      String patientId,
      String codeChallengeMethod,
      String codeChallenge
  ) {
    JSONObject code = new JSONObject();

    if (actualCode != null) {
      code.put("code", actualCode);
    }
    if (scopes != null) {
      code.put("scopes", scopes);
    }
    if (patientId != null) {
      code.put("patientId", patientId);
    }
    if (codeChallengeMethod != null) {
      code.put("codeChallengeMethod", codeChallengeMethod);
    }
    if (codeChallenge != null) {
      code.put("codeChallenge", codeChallenge);
    }

    return Base64.getEncoder().encodeToString(code.toString().getBytes());
  }

  /**
   * Create the Authorization Header value.
   * 
   * @param accessToken the access token value
   * @return string representation of the Authorization Header Value
   */
  public static String createAuthorizationHeaderValue(String accessToken) {
    return BEARER_TOKEN_PREFIX + " " + accessToken;
  }

  /**
   * converts scope string to scope list.
   * 
   * @param scopesString scopes separated by a space
   * @return List of strings representation of the input string containing scopes
   */
  public static List<String> getScopesListByScopeString(String scopesString) {

    if (scopesString == null) {
      return new ArrayList<>();
    }

    String[] scopesArray = scopesString.trim().split("\\s+");

    return Arrays.stream(scopesArray).filter(scope -> !"".equals(scope))
        .collect(Collectors.toList());
  }

  /**
   * converts scope list to scope string.
   * 
   * @param scopesList List with each scope String
   * @return string representation of the input list of scopes
   */
  public static String getScopesStringFromScopesList(List<String> scopesList) {
    if (scopesList == null) {
      return "";
    }

    return String.join(" ", scopesList);
  }

  /**
   * gets or creates a FHIR client whose base url matches that of the request.
   *
   * @param theRequest HttpServletRequest whose base represents a FHIR endpoint
   * @return IGGenericClient client for accessing that FHIR endpoint
   */
  public static IGenericClient getClientFromRequest(HttpServletRequest theRequest) {
    String fhirServerBaseUrl = getServerBaseUrl(theRequest) + FHIR_SERVER_PATH;
    if (clients.containsKey(fhirServerBaseUrl)) {
      return clients.get(fhirServerBaseUrl);
    }

    IGenericClient newClient = FHIR_CONTEXT_R4.newRestfulGenericClient(fhirServerBaseUrl);
    clients.put(fhirServerBaseUrl, newClient);

    return newClient;
  }

  private FhirReferenceServerUtils() {
  }
}
