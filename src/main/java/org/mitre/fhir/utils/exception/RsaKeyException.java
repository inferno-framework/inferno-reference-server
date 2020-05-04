package org.mitre.fhir.utils.exception;

public class RsaKeyException extends Exception {

  private static final long serialVersionUID = 1L;

  public RsaKeyException(String errorMessage, Exception exception) {
    super(errorMessage, exception);
  }


}
