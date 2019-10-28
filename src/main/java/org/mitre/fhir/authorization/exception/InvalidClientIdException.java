package org.mitre.fhir.authorization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


public class InvalidClientIdException extends ResponseStatusException {

	private static final String ERROR_MESSAGE = "Invalid Client Id.";

	private static final long serialVersionUID = 1L;

	public InvalidClientIdException(String clientId) {
		super(HttpStatus.UNAUTHORIZED, ERROR_MESSAGE + " Supplied Client ID: " + clientId);
	}
}
