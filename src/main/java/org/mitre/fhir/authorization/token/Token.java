package org.mitre.fhir.authorization.token;

import java.util.List;
import java.util.UUID;

public class Token {

  private boolean active = true;
  private final List<String> scopes;
  private String patientId;
  private String encounterId;

  private final String tokenValue;

  /**
   * Token constructor.
   * 
   * @param scopes scopes this token is authorized to access
   */
  public Token(List<String> scopes) {
    this(UUID.randomUUID().toString(), scopes);
  }

  public Token(String tokenValue, List<String> scopes) {
    this.tokenValue = tokenValue;
    this.scopes = scopes;
  }

  public void revokeToken() {
    active = false;
  }

  public boolean isActive() {
    return active;
  }

  public String getTokenValue() {
    return tokenValue;
  }

  public List<String> getScopes() {
    return scopes;
  }

  /**
   * Gets the list of scopes as a string.
   * 
   * @return a string of each scope separated by a space
   */
  public String getScopesString() {
    StringBuilder scopesString = new StringBuilder();
    for (String scope : scopes) {
      scopesString.append(scope).append(" ");
    }

    return scopesString.toString().strip();
  }

  public void setPatientId(String patientId) {
    this.patientId = patientId;
  }

  public void setEncounterId(String encounterId) {
    this.encounterId = encounterId;
  }

  public String getPatientId() {
    return patientId;
  }

  public String getEncounterId() {
    return encounterId;
  }
}
