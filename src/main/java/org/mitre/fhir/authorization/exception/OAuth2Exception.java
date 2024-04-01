package org.mitre.fhir.authorization.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

/**
 * Exception to represent errors in OAuth2 processes.
 * See https://datatracker.ietf.org/doc/html/rfc6749#section-5.2 .
 */
public class OAuth2Exception extends RuntimeException {
  private static final long serialVersionUID = 1L;
  
  private final String error;
  private final String errorDescription;
  
  private HttpStatus responseStatus = HttpStatus.BAD_REQUEST;
  private HttpHeaders responseHeaders;
  
  public OAuth2Exception(ErrorCode error) {
    this(error, null, null);
  }
  
  public OAuth2Exception(ErrorCode error, Throwable cause) {
    this(error, cause.getMessage(), cause);
  }
  
  public OAuth2Exception(ErrorCode error, String errorDescription) {
    this(error, errorDescription, null);
  }

  /**
   * Constructor for an OAuth2Exception with all fields.
   * @param error The type of error
   * @param errorDescription Additional text that may aid the recipient of the error
   * @param cause The exception that caused this one, if any
   */
  public OAuth2Exception(ErrorCode error, String errorDescription, Throwable cause) {
    super(cause);
    this.error = error.toString().toLowerCase();
    this.errorDescription = errorDescription;
  }
  
  /**
   * Set the HTTP response code that will be used when this exception is returned.
   * Defaults to HttpStatus.BAD_REQUEST if not set.
   * @param status HTTP Status
   * @return this exception, for chaining
   */
  public OAuth2Exception withResponseStatus(HttpStatus status) {
    if (status != null) {
      this.responseStatus = status;
    }
    return this;
  }
  
  /**
   * Set the given key/value as a response header when this exception is returned.
   * @param key Response header name
   * @param value Response header value
   * @return this exception, for chaining
   */
  public OAuth2Exception withHeader(String key, String value) {
    if (this.responseHeaders == null) {
      this.responseHeaders = new HttpHeaders();
    }
    this.responseHeaders.add(key, value);
    return this;
  }
  
  public String getError() {
    return error;
  }

  public String getErrorDescription() {
    return errorDescription;
  }
  
  public HttpStatus getResponseStatus() {
    return responseStatus;
  }
  
  public HttpHeaders getResponseHeaders() {
    return responseHeaders;
  }

  public static enum ErrorCode {
    /**
     * The request is missing a required parameter, includes an unsupported
     * parameter value (other than grant type), repeats a parameter, includes
     * multiple credentials, utilizes more than one mechanism for authenticating the
     * client, or is otherwise malformed.
     */
    INVALID_REQUEST,

    /**
     * Client authentication failed (e.g., unknown client, no client authentication
     * included, or unsupported authentication method). The authorization server MAY
     * return an HTTP 401 (Unauthorized) status code to indicate which HTTP
     * authentication schemes are supported. If the client attempted to authenticate
     * via the "Authorization" request header field, the authorization server MUST
     * respond with an HTTP 401 (Unauthorized) status code and include the
     * "WWW-Authenticate" response header field matching the authentication scheme
     * used by the client.
     */
    INVALID_CLIENT,

    /**
     * The provided authorization grant (e.g., authorization code, resource owner
     * credentials) or refresh token is invalid, expired, revoked, does not match
     * the redirection URI used in the authorization request, or was issued to
     * another client.
     */
    INVALID_GRANT,

    /**
     * The authenticated client is not authorized to use this authorization grant
     * type.
     */
    UNAUTHORIZED_CLIENT,

    /**
     * The authorization grant type is not supported by the authorization server.
     */
    UNSUPPORTED_GRANT_TYPE,

    /**
     * The requested scope is invalid, unknown, malformed, or exceeds the scope
     * granted by the resource owner.
     */
    INVALID_SCOPE,


    /**
     * Used to indicate that the server failed to handle a valid request.
     * NOTE:
     * server_error is not technically valid for the "error" field on an error response
     * from the /token endpoint, however there is no guidance on what should happen
     * in these cases, and server_error is used for the authorization endpoint
     * (since a redirect cannot return HTTP error codes)
     */
    SERVER_ERROR,

  }
  
}
