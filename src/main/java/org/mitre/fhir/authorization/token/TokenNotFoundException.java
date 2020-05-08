package org.mitre.fhir.authorization.token;

public class TokenNotFoundException extends Exception {

  private static final long serialVersionUID = 2094188933622684896L;

  public TokenNotFoundException(String tokenValue) {
    super("Token " + tokenValue + " not found");
  }

}
