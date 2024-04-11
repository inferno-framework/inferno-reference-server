package org.mitre.fhir.bulk;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;

@Interceptor
public class BulkInterceptor {

  private static final String DELETE = "DELETE";
  private static final String EXPORT_POLL_STATUS_PATH_INFO =
      "/" + ProviderConstants.OPERATION_EXPORT_POLL_STATUS;
  private static final String EXPORT_PATH_INFO = "/" + ProviderConstants.OPERATION_EXPORT;

  private static Set<String> cancelledJobs = Collections.synchronizedSet(new HashSet<String>());
  
  // TODO: make this @Autowired if possible
  private FhirContext fhirContext;
  
  public BulkInterceptor(FhirContext fhirContext) {
    this.fhirContext = fhirContext;
  }

  /**
   * Interceptor method to address a couple limitations in HAPI's bulk export service.
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
  public boolean incomingRequestPreProcessed(HttpServletRequest theRequest,
      HttpServletResponse theResponse) throws ServletException, IOException {
    String pathInfo = theRequest.getPathInfo();
    if (pathInfo == null) {
      return true;
    } else if (pathInfo.equals(EXPORT_POLL_STATUS_PATH_INFO)) {
      String jobId = theRequest.getParameter(JpaConstants.PARAM_EXPORT_POLL_STATUS_JOB_ID);
      if (theRequest.getMethod().equals(DELETE)) {
        cancelledJobs.add(jobId);
        return true;
      } else if (theRequest.getMethod().equals("GET") && cancelledJobs.contains(jobId)) {
        // override the response to be a 404
        // hapi currently returns a 202 with header:
        // X-Progress="Build in progress - Status set to CANCELLED at (time)"
        theResponse.setStatus(ca.uhn.fhir.rest.api.Constants.STATUS_HTTP_404_NOT_FOUND);

        IBaseOperationOutcome oo = OperationOutcomeUtil.newInstance(fhirContext);
        OperationOutcomeUtil.addIssue(fhirContext, oo, "error",
                                      "Bulk export job " + jobId + "not found", null, "not-found");
        fhirContext
            .newJsonParser()
            .setPrettyPrint(true)
            .encodeResourceToWriter(oo, theResponse.getWriter());
        theResponse.getWriter().close();

        return false;
      }
    } else if (pathInfo.endsWith(EXPORT_PATH_INFO)) {
      String outputFormat = theRequest.getParameter(JpaConstants.PARAM_EXPORT_OUTPUT_FORMAT);
      if ("application/ndjson".equals(outputFormat) || "ndjson".equals(outputFormat)) {
        // rewrite it - hapi doesn't support these but they are SHALL-accept in the spec
        
        String queryString = theRequest.getQueryString()
            .replace(URLEncoder.encode(outputFormat, "UTF-8"), URLEncoder.encode("application/fhir+ndjson", "UTF-8"));
        String newPath = theRequest.getServletPath() + theRequest.getPathInfo()  + "?" + queryString;
        RequestDispatcher requestDispatcher = theRequest.getRequestDispatcher(newPath);
        requestDispatcher.forward(theRequest, theResponse);
        return false;
      }
    }

    // if we get here, continue with usual processing
    return true;
  }
}
