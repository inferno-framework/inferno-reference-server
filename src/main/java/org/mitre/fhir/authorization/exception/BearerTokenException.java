package org.mitre.fhir.authorization.exception;

public class BearerTokenException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	private static final String DEFAULT_ERROR_MESSAGE = "Error generating Bearer token";

	public BearerTokenException(String errorMessage, Exception exception)
	{
		super(errorMessage, exception);
	}
	
	public BearerTokenException(Exception exception)
	{
		this(DEFAULT_ERROR_MESSAGE, exception);
	}
	
	

}
