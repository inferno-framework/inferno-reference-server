package org.mitre.fhir.bulk;

import static java.nio.charset.StandardCharsets.UTF_8;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.interceptor.api.Hook;
import ca.uhn.fhir.interceptor.api.Interceptor;
import ca.uhn.fhir.interceptor.api.Pointcut;
import ca.uhn.fhir.jpa.bulk.export.model.BulkExportResponseJson;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.rest.server.provider.ProviderConstants;
import ca.uhn.fhir.util.JsonUtil;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.mitre.fhir.utils.FhirReferenceServerUtils;

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
    }

    Boolean hackReturnVal = applyTemporaryOverrideLogic(theRequest, theResponse);

    if (hackReturnVal != null) {
      return hackReturnVal;
    }

    if (pathInfo.equals(EXPORT_POLL_STATUS_PATH_INFO)) {
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

        String oldFormatEnc = URLEncoder.encode(outputFormat, UTF_8);
        String newFormatEnc = URLEncoder.encode("application/fhir+ndjson", UTF_8);
        String queryStr = theRequest.getQueryString().replace(oldFormatEnc, newFormatEnc);
        String newPath = theRequest.getServletPath() + theRequest.getPathInfo()  + "?" + queryStr;
        RequestDispatcher requestDispatcher = theRequest.getRequestDispatcher(newPath);
        requestDispatcher.forward(theRequest, theResponse);
        return false;
      }
    }

    // if we get here, continue with usual processing
    return true;
  }

  /*---------------------------------------------------------------------------
   * Everything below this line is a temporary hack
   * to include Location resources in Bulk Data Group exports.
   * See: https://github.com/hapifhir/hapi-fhir/issues/6443
   *
   * The basic approach is just serve up pre-cached results.
   * More specifically, we watch when an export request is made on
   * our Group of interest and capture the outbound job ID in the response.
   * Future GET calls to poll the status of that job ID will be overwritten
   * with links to our cached results.
   * DELETEing the job will delete the real job behind the scenes and so we
   * don't need to change anything in that flow.
   *
   * Remove everything below this once that fix is merged
   * and we upgrade to the corresponding version of HAPI.
   * (Plus remove the function call above and unused imports)
   *---------------------------------------------------------------------------
   */

  /**
   * Cache the results of a Bulk Export for the given Group.
   * @param groupId Id for the Group, eg "1a" not "Group/1a"
   * @param ids Identifiers for the cached Binary exports plus the associated resource types.
   *            Consider it a list of tuples (resourceType, Binary resource id)
   */
  public static void cacheGroupBulkExport(String groupId, String[][] ids) {
    CACHED_BULK_EXPORT.put(groupId, ids);
  }

  private static final Map<String, String[][]> CACHED_BULK_EXPORT = new HashMap<>();

  private static String exportPathForGroup(String groupId) {
    return "/Group/" + groupId + "/$export";
  }

  /**
   * Job IDs that we want to intercept, mapped to the group ID they are exporting.
   */
  private static Map<String, String> interceptedJobs =
      Collections.synchronizedMap(new HashMap<String, String>());

  /**
   * Helper method to apply the overrides. This fits into incomingRequestPreProcessed
   * above but was split out just to make removing it later easier.
   */
  private Boolean applyTemporaryOverrideLogic(HttpServletRequest theRequest,
      HttpServletResponse theResponse) throws IOException {
    String pathInfo = theRequest.getPathInfo();

    if (pathInfo.equals(EXPORT_POLL_STATUS_PATH_INFO)) {
      String jobId = theRequest.getParameter(JpaConstants.PARAM_EXPORT_POLL_STATUS_JOB_ID);
      if (theRequest.getMethod().equals("GET")
          && interceptedJobs.containsKey(jobId)
          && !cancelledJobs.contains(jobId)) {
        // override the response

        String baseUrl = FhirReferenceServerUtils.getFhirServerBaseUrl(theRequest);

        // generate the response the same way HAPI does.
        // see ca.uhn.fhir.batch2.jobs.export.BulkDataExportProvider#exportPollStatus
        BulkExportResponseJson bulkResponseDocument = new BulkExportResponseJson();
        bulkResponseDocument.setTransactionTime(new Date());
        bulkResponseDocument.setRequiresAccessToken(true);

        String groupId = interceptedJobs.get(jobId);

        // original request URL
        bulkResponseDocument.setRequest(baseUrl + exportPathForGroup(groupId));

        String[][] cachedBinaryIds = CACHED_BULK_EXPORT.get(groupId);
        for (String[] binary : cachedBinaryIds) {
          String resourceType = binary[0];
          String id = binary[1];
          bulkResponseDocument.addOutput()
              .setType(resourceType)
              .setUrl(baseUrl + "/Binary/" + id);
        }

        theResponse.addHeader("Content-Type", "application/json");

        JsonUtil.serialize(bulkResponseDocument, theResponse.getWriter());
        theResponse.getWriter().close();

        return false;
      }
    }

    // nothing to do to the current request
    return null;
  }

  /**
   * Hook for the outgoing response -- when our $export of interest has been hit,
   * read the Content-Location header on the response to get the job ID.
   */
  @Hook(Pointcut.SERVER_OUTGOING_RESPONSE)
  public void handleOutgoingResponse(HttpServletRequest theRequest,
      HttpServletResponse theResponse) {
    String pathInfo = theRequest.getPathInfo();

    for (String groupId : CACHED_BULK_EXPORT.keySet()) {
      String exportPath = exportPathForGroup(groupId);
      if (exportPath.equals(pathInfo)) {
        // use a custom header to allow us to override this interceptor,
        // eg, if we want to run the "cache data" script against this server itself
        String bypassHack = theRequest.getHeader("X-Override-Interceptor");
        if (bypassHack != null) {
          return;
        }

        String contentLocation = theResponse.getHeader("Content-Location");
        // expected to always be something like
        // http://localhost:8080/reference-server/r4/$export-poll-status?_jobId=64327c6f-6bc5-440b-bce3-5ca4dda278f5
        // we just want the jobId at the end.
        String jobId = contentLocation.substring(contentLocation.length() - 36);
        interceptedJobs.put(jobId, groupId);
      }
    }
  }
}
