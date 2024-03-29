package org.mitre.fhir.bulk;

import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import java.io.IOException;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

@Interceptor
public class BulkInterceptor {

  private static final String DELETE = "DELETE";
  private static final String EXPORT_POLL_STATUS_PATH_INFO =
      "/" + JpaConstants.OPERATION_EXPORT_POLL_STATUS;
  private static final String POST = "POST";

  /**
   * Handles redirecting DELETE to correct operation.
   * 
   */
  @Hook(Pointcut.SERVER_INCOMING_REQUEST_PRE_PROCESSED)
  public boolean incomingRequestPreProcessed(HttpServletRequest theRequest,
      HttpServletResponse theResponse) throws ServletException, IOException {
    // change the request if it is DELETE
    if (theRequest.getMethod().equals(DELETE)
        && EXPORT_POLL_STATUS_PATH_INFO.equals(theRequest.getPathInfo())) {
      String newUri = "$bulk-delete";
      RequestDispatcher requestDispatcher = theRequest.getRequestDispatcher(newUri);
      HttpServletRequest newServletRequest = new HttpServletRequestWrapper(theRequest) {
        // Change Request Method to POST
        @Override
        public String getMethod() {
          return POST;
        }
      };

      requestDispatcher.forward(newServletRequest, theResponse);

      // no need to continue with the request.
      return false;
    }

    return true;
  }
}
