package org.mitre.fhir.bulk;

import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.springframework.web.bind.annotation.DeleteMapping;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;


public class BulkFunctionsNotSupportedByHapiController {

  @DeleteMapping(path = "authorizeClientId/{clientId}", produces = {"application/json"})
  public void exportPollStatus(
      @OperationParam(name = JpaConstants.PARAM_EXPORT_POLL_STATUS_JOB_ID, typeName = "string",
          min = 0, max = 1) IPrimitiveType<String> theJobId,
      ServletRequestDetails theRequestDetails)  {
    
  }
}
