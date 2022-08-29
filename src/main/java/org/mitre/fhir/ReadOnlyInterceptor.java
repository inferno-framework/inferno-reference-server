package org.mitre.fhir;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;

@Interceptor
public class ReadOnlyInterceptor {

  /**
   * Handles prevention of non-read operations.
   *
   */
  @Hook(SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void incomingRequestPreProcessed(RestOperationTypeEnum theOperation) {
    if (theOperation != RestOperationTypeEnum.HISTORY_INSTANCE
            && theOperation != RestOperationTypeEnum.HISTORY_SYSTEM
            && theOperation != RestOperationTypeEnum.HISTORY_TYPE
            && theOperation != RestOperationTypeEnum.METADATA
            && theOperation != RestOperationTypeEnum.READ
            && theOperation != RestOperationTypeEnum.SEARCH_SYSTEM
            && theOperation != RestOperationTypeEnum.SEARCH_TYPE
            && theOperation != RestOperationTypeEnum.TRANSACTION
            && theOperation != RestOperationTypeEnum.VALIDATE
            && theOperation != RestOperationTypeEnum.VREAD) {
      throw new MethodNotAllowedException("Server is currently `read-only`: the "
              + theOperation.toString()
              + " operation is not allowed.");
    }
  }
}



