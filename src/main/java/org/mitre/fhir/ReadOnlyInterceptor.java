package org.mitre.fhir;

import static ca.uhn.fhir.interceptor.api.Pointcut.SERVER_INCOMING_REQUEST_PRE_HANDLED;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.RestOperationTypeEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.exceptions.MethodNotAllowedException;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import java.util.EnumSet;
import java.util.Set;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;

@Interceptor
public class ReadOnlyInterceptor {

  /**
   * White list of operation types that are allowed while the server is read-only.
   * Batches and transactions (TRANSACTION) are handled separately in incomingRequestPreProcessed.
   */
  private static final Set<RestOperationTypeEnum> ALLOWED_OPERATION_TYPES = EnumSet.of(
      RestOperationTypeEnum.HISTORY_INSTANCE,
      RestOperationTypeEnum.HISTORY_SYSTEM,
      RestOperationTypeEnum.HISTORY_TYPE,
      RestOperationTypeEnum.METADATA,
      RestOperationTypeEnum.READ,
      RestOperationTypeEnum.SEARCH_SYSTEM,
      RestOperationTypeEnum.SEARCH_TYPE,
      RestOperationTypeEnum.VALIDATE,
      RestOperationTypeEnum.VREAD,
      RestOperationTypeEnum.GET_PAGE,
      // Extended operations are further restricted by name, see ALLOWED_EXTENDED_OPERATIONS
      RestOperationTypeEnum.EXTENDED_OPERATION_SERVER,
      RestOperationTypeEnum.EXTENDED_OPERATION_TYPE,
      RestOperationTypeEnum.EXTENDED_OPERATION_INSTANCE);

  private static final Set<String> ALLOWED_EXTENDED_OPERATIONS = Set.of(
      ProviderConstants.OPERATION_EXPORT,
      ProviderConstants.OPERATION_EXPORT_POLL_STATUS,
      Constants.EXTOP_VALIDATE,
      JpaConstants.OPERATION_EVERYTHING,
      JpaConstants.OPERATION_GET_RESOURCE_COUNTS,
      ProviderConstants.OPERATION_META,
      JpaConstants.OPERATION_DOCUMENT,
      JpaConstants.OPERATION_LASTN,
      JpaConstants.OPERATION_SNAPSHOT,
      JpaConstants.OPERATION_EXPAND,
      JpaConstants.OPERATION_LOOKUP,
      JpaConstants.OPERATION_SUBSUMES,
      JpaConstants.OPERATION_TRANSLATE,
      JpaConstants.OPERATION_VALIDATE_CODE);

  /**
   * Handles prevention of operations that may modify server data.
   *
   */
  @Hook(SERVER_INCOMING_REQUEST_PRE_HANDLED)
  public void incomingRequestPreProcessed(RestOperationTypeEnum theOperation,
      RequestDetails theRequestDetails) {
    String operationName = theRequestDetails.getOperation();

    if (theOperation == RestOperationTypeEnum.TRANSACTION) {
      Bundle bundle = (Bundle) theRequestDetails.getResource();

      for (BundleEntryComponent entry : bundle.getEntry()) {
        if (entry.getRequest().getMethod() != HTTPVerb.GET) {
          throw new MethodNotAllowedException("Server is currently `read-only`: "
              + "batches and transactions may only contain GET requests.");
        }
      }
    } else if (!ALLOWED_OPERATION_TYPES.contains(theOperation)) {
      throw new MethodNotAllowedException("Server is currently `read-only`: the "
          + theOperation + " operation is not allowed.");
    } else if (operationName != null && operationName.startsWith("$")
        && !ALLOWED_EXTENDED_OPERATIONS.contains(operationName)) {
      throw new MethodNotAllowedException("Server is currently `read-only`: the "
          + operationName + " operation is not allowed.");
    }
  }
}
