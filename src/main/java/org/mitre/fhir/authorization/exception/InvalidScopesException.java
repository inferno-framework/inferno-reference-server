package org.mitre.fhir.authorization.exception;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;

public class InvalidScopesException extends BaseServerResponseException {

	private static final int HTTP_RESPONSE_CODE_UNAUTHORIZED = 401;

	private static final long serialVersionUID = 1L;

	public InvalidScopesException(String resource) {
		super(HTTP_RESPONSE_CODE_UNAUTHORIZED, "Access to Resource " + resource + " is restricted due to invalid scopes");
	}
}
