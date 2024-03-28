package org.mitre.fhir.authorization.exception;

import org.springframework.http.HttpStatus;

public class InvalidClientIdException extends OAuth2Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Create an InvalidClientIdException for the given clientID.
   * @param clientId client_id that was supplied
   * @param basicAuth Whether or not this client attempted to authenticate via the
   *                  "Authorization" request header field. See
   *                  https://datatracker.ietf.org/doc/html/rfc6749#section-5.2
   */
  public InvalidClientIdException(String clientId, boolean basicAuth) {
    super(ErrorCode.INVALID_CLIENT, "Invalid Client Id. Supplied Client ID: \"" + clientId + "\"");
    withResponseStatus(HttpStatus.UNAUTHORIZED);
    if (basicAuth) {
      withHeader("WWW-Authenticate", "Basic");
    }
  }
}
