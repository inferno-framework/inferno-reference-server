package gov.onc.authorization.exception;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

public class InvalidBearerTokenException extends BaseServerResponseException {
	
	private static final String ERROR_MESSAGE = "Bearer token is invalid or not supplied"; 
	private static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = 401;
	
	private static final long serialVersionUID = 1L;

	public InvalidBearerTokenException()
	{
		super(HTTP_RESPONSE_CODE_UNAUTHORIZED, ERROR_MESSAGE);
	}
	
	public InvalidBearerTokenException(String bearerToken)
	{
		super(HTTP_RESPONSE_CODE_UNAUTHORIZED,ERROR_MESSAGE + " Supplied Bearer Token: " + bearerToken);
	}
}
