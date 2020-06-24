package org.mitre.fhir.authorization.token;

import java.util.List;
import java.util.UUID;

public class Token {

  private boolean active = true;
  private List<String> scopes;
  private String patientId;

  private String tokenValue;

  public Token(List<String> scopes) {
    UUID uuid = UUID.randomUUID();
    this.tokenValue = uuid.toString();
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
  
  public List<String> getScopes()
  {
    return scopes;
  }
  
  public String getScopesString()
  {
    String scopesString = "";
    for (String scope : scopes)
    {
      scopesString += scope + " ";
    }
    
    scopesString = scopesString.strip();
    
    return scopesString;
    
  }
  
  public void setPatientId(String patientId)
  {
    this.patientId = patientId;
  }
  
  public String getPatientId()
  {
    return patientId;
  }

}
