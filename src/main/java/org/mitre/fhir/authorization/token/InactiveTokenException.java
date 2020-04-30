package org.mitre.fhir.authorization.token;

public class InactiveTokenException extends Exception {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -3357569445994575841L;

	public InactiveTokenException(Token token)
	{
		super("Token " + token.getTokenValue() + " is not active.");
	}

}