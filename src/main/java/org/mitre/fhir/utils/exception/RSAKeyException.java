package org.mitre.fhir.utils.exception;

public class RSAKeyException extends Exception {

  private static final long serialVersionUID = 1L;

  public RSAKeyException(String errorMessage, Exception exception) {
    super(errorMessage, exception);
  }


}
