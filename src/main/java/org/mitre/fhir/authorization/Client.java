package org.mitre.fhir.authorization;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simplified model of a SMART-on-FHIR (OAuth2) client registration,
 * as well as a repository of Client instances.
 * This implementation was cloned from the OAuth2 Client Registration model from Spring Security
 * https://github.com/spring-projects/spring-security/blob/main/oauth2/oauth2-client/src/main/java/org/springframework/security/oauth2/client/registration/ClientRegistration.java
 * Note: fields and constants have been limited to only those currently supported in this server,
 * so this does not represent a complete model of a SMART-on-FHIR client.
 * All new custom settings specific to the reference-server are in the CustomSettings subclass.
 */
public class Client implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private static final Map<String, Client> ALL_CLIENTS = new HashMap<>();
  
  static {
    // Load up the default clients automatically. 
    // Loading them here means they are accessible to unit tests.
    try {
      URI defaultClientsResource = Client.class.getResource("/default_clients.json").toURI();
      Path defaultClientsPath = Paths.get(defaultClientsResource);
      load(defaultClientsPath);
    } catch (Exception e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  /////////////////////
  // INSTANCE FIELDS //
  /////////////////////
  private String clientId;

  private String clientSecret;

  private String clientAuthenticationMethod;

  private String authorizationGrantType;

  private ProviderDetails providerDetails;

  private CustomSettings customSettings;

  ////////////////////////
  // REPOSITORY METHODS //
  ////////////////////////
  
  /**
   * Register a new Client in the repository.
   * @param c Client to register
   * @throws IllegalArgumentException if a client with the given client_id already exists
   */
  public static void register(Client c) {
    if (ALL_CLIENTS.containsKey(c.clientId)) {
      throw new IllegalArgumentException("Client with client_id " + c.clientId + " already exists");
    }
    
    ALL_CLIENTS.put(c.clientId, c);
  }
  
  /**
   * Find the Client with the given client_id.
   */
  public static Client find(String clientId) {
    return ALL_CLIENTS.get(clientId);
  }
  
  /**
   * Load and register Clients defined in a JSON file or folder at the given Path.
   */
  public static void load(Path clientsPath) {
    File pathAsFile = clientsPath.toFile();
    File[] files;
    if (pathAsFile.isDirectory()) {
      files = pathAsFile.listFiles((d, name) -> name.endsWith(".json"));
    } else if (pathAsFile.exists()) {
      files = new File[] { pathAsFile };
    } else {
      throw new RuntimeException("Unable to load clients from " + clientsPath
          + ". Please make sure the path exists and is readable");
    }
    
    Arrays.sort(files); // sort files for consistent and predictable ordering
    Gson gson = new Gson();
    int errorCount = 0;
    for (File file : files) {
      try {
        String jsonString = Files.readString(file.toPath());
        // note we also use HAPI's JsonParser, so fully-qualified classname is required here
        JsonElement jsonElement = com.google.gson.JsonParser.parseString(jsonString);

        if (jsonElement.isJsonArray()) {
          Client[] clients = gson.fromJson(jsonElement, Client[].class);
          for (Client c : clients) {
            Client.register(c);
          }
        } else {
          Client c = gson.fromJson(jsonElement, Client.class);
          Client.register(c);
        }

      } catch (Exception e) {
        System.err.println("Failed to load client definitions from " + file.getName());
        e.printStackTrace();
        errorCount++;
      }
    }
    if (errorCount > 0) {
      String message = "Unable to load client definitions from " + clientsPath
          + ". Review the previous " + errorCount + " exceptions for details";
      throw new RuntimeException(message);
    }
  }

  //////////////////////////////////////
  // INTERNAL DATA STRUCTURE CLASSES //
  /////////////////////////////////////
  
  public static class ProviderDetails {
    private String jwkSetUri;

    public String getJwkSetUri() {
      return jwkSetUri;
    }

    public void setJwkSetUri(String jwkSetUri) {
      this.jwkSetUri = jwkSetUri;
    }
  }
  
  /**
   * Custom settings specific to this server implementation.
   * For now the only custom setting is to limit the patients
   * that are displayed in the SMART launch patient picker UI.
   */
  public static class CustomSettings {
    /**
     * List of Patient IDs that will be displayed in the SMART launch patient picker UI.
     * Note that the UI does not create records, so if Patients with the given IDs do not exist,
     * then the picker will be empty.
     */
    private List<String> patientPickerIds;

    public List<String> getPatientPickerIds() {
      return patientPickerIds;
    }

    public void setPatientPickerIds(List<String> patientPickerIds) {
      this.patientPickerIds = patientPickerIds;
    }
  }
  
  ///////////
  // ENUMS //
  ///////////
  
  /**
   * Simplified enumeration of authentication methods.
   */
  public final class ClientAuthenticationMethod {
    /**
     * client_secret_basic auth method.
     * Implies a Confidential Symmetric client.
     */
    public static final String CLIENT_SECRET_BASIC = "client_secret_basic";

    /**
     * private_key_jwt auth method.
     * Implies a Confidential Asymmetric client.
     */
    public static final String PRIVATE_KEY_JWT = "private_key_jwt";

    /**
     * "none" type auth method.
     * Implies a Public client.
     */
    public static final String NONE = "none";
  }

  /**
   * Simplified enumeration of authorization grant types.
   */
  public static final class AuthorizationGrantType {
    /**
     * authorization_code grant type.
     * Implies a Standalone launch client.
     */
    public static final String AUTHORIZATION_CODE = "authorization_code";
    
    /**
     * client_credentials grant type.
     * Implies a 
     */
    public static final String CLIENT_CREDENTIALS = "client_credentials";
  }
  
  /////////////////////////////
  // PLAIN GETTERS & SETTERS //
  /////////////////////////////
  
  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getClientAuthenticationMethod() {
    return clientAuthenticationMethod;
  }

  public void setClientAuthenticationMethod(String clientAuthenticationMethod) {
    this.clientAuthenticationMethod = clientAuthenticationMethod;
  }

  public String getAuthorizationGrantType() {
    return authorizationGrantType;
  }

  public void setAuthorizationGrantType(String authorizationGrantType) {
    this.authorizationGrantType = authorizationGrantType;
  }

  public ProviderDetails getProviderDetails() {
    return providerDetails;
  }

  public void setProviderDetails(ProviderDetails providerDetails) {
    this.providerDetails = providerDetails;
  }

  public CustomSettings getCustomSettings() {
    return customSettings;
  }

  public void setCustomSettings(CustomSettings customSettings) {
    this.customSettings = customSettings;
  }

}
