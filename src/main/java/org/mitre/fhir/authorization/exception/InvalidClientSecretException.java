package org.mitre.fhir.authorization.exception;

import org.springframework.http.HttpStatus;

public class InvalidClientSecretException extends OAuth2Exception {
  private static final String ERROR_MESSAGE = "Client Secret invalid or not supplied";

  private static final long serialVersionUID = 1L;

  public InvalidClientSecretException() {
    super(OAuth2Exception.ErrorCode.INVALID_CLIENT, ERROR_MESSAGE);
    withResponseStatus(HttpStatus.UNAUTHORIZED);
  }
}
