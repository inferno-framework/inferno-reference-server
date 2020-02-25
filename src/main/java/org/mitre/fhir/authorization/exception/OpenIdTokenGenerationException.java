package org.mitre.fhir.authorization.exception;

public class OpenIdTokenGenerationException extends Exception {
	
	private static final long serialVersionUID = 1L;
	
	private static final String DEFAULT_ERROR_MESSAGE = "Error generating OpenId token";

	public OpenIdTokenGenerationException(String errorMessage, Exception exception)
	{
		super(errorMessage, exception);
	}
	
	public OpenIdTokenGenerationException(Exception exception)
	{
		this(DEFAULT_ERROR_MESSAGE, exception);
	}
	
	

}
