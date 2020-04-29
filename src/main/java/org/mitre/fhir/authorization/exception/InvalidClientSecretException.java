package org.mitre.fhir.authorization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidClientSecretException extends ResponseStatusException {
  private static final String ERROR_MESSAGE = "Client Secret invalid or not supplied";

  private static final long serialVersionUID = 1L;

  public InvalidClientSecretException() {
    super(HttpStatus.UNAUTHORIZED, ERROR_MESSAGE);
  }
}
