package org.mitre.fhir.bulk;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.bulk.export.api.BulkDataExportOptions;
import ca.uhn.fhir.jpa.bulk.export.api.IBulkDataExportSvc;
import ca.uhn.fhir.jpa.bulk.export.model.BulkExportResponseJson;
import ca.uhn.fhir.jpa.bulk.export.provider.BulkDataExportProvider;
import ca.uhn.fhir.jpa.model.util.JpaConstants;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.api.CacheControlDirective;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.PreferHeader;
import ca.uhn.fhir.rest.server.RestfulServerUtils;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.servlet.ServletRequestDetails;
import ca.uhn.fhir.util.ArrayUtil;
import ca.uhn.fhir.util.JsonUtil;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseOperationOutcome;
import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.InstantType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.IssueSeverity;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;
import org.springframework.beans.factory.annotation.Autowired;
import com.google.common.annotations.VisibleForTesting;

/**
 * Bulk Data Export Provider with Required Fhir Authentication. this is copied from
 * BulkDataExportProvider.java to edit it's behavior
 * 
 * @author hershil
 *
 */
public class AuthorizationBulkDataExportProvider {

  private static final String ACCEPT = "accept";
  private static final List<String> VALID_ACCEPT_HEADERS = new ArrayList<String>();

  public static final String FARM_TO_TABLE_TYPE_FILTER_REGEX = "(?:,)(?=[A-Z][a-z]+\\?)";
  private static final Logger ourLog = getLogger(BulkDataExportProvider.class);

  @Autowired
  private IBulkDataExportSvc myBulkDataExportSvc;

  @Autowired
  private FhirContext myFhirContext;

  private static final String[] DEFAULT_RESOURCE_TYPES = {"Patient", "AllergyIntolerance",
      "CarePlan", "CareTeam", "Condition", "Device", "DiagnosticReport", "DocumentReference",
      "Goal", "Immunization", "MedicationRequest", "Observation", "Procedure", "Encounter",
      "Organization", "Practitioner", "Provenance", "Location", "Medication"};

  static {
    VALID_ACCEPT_HEADERS.add("application/fhir+json");
  }

  @VisibleForTesting
  public void setFhirContextForUnitTest(FhirContext theFhirContext) {
    myFhirContext = theFhirContext;
  }

  @VisibleForTesting
  public void setBulkDataExportSvcForUnitTests(IBulkDataExportSvc theBulkDataExportSvc) {
    myBulkDataExportSvc = theBulkDataExportSvc;
  }

  /**
   * $export function.
   */
  @Operation(name = JpaConstants.OPERATION_EXPORT,
      global = false /* set to true once we can handle this */, manualResponse = true,
      idempotent = true)
  public void export(
      @OperationParam(name = JpaConstants.PARAM_EXPORT_OUTPUT_FORMAT, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theOutputFormat,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_TYPE, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theType,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_SINCE, min = 0, max = 1,
          typeName = "instant") IPrimitiveType<Date> theSince,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_TYPE_FILTER, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theTypeFilter,
      ServletRequestDetails theRequestDetails) {

    try {
      authorize(theRequestDetails);
      validatePreferAsyncHeader(theRequestDetails);
    }

    catch (InvalidRequestException invalidRequestException) {
      handleInvalidRequestException(invalidRequestException, theRequestDetails);
      return;
    }

    BulkDataExportOptions bulkDataExportOptions =
        buildSystemBulkExportOptions(theOutputFormat, theType, theSince, theTypeFilter);
    Boolean useCache = shouldUseCache(theRequestDetails);
    IBulkDataExportSvc.JobInfo outcome =
        myBulkDataExportSvc.submitJob(bulkDataExportOptions, useCache);
    writePollingLocationToResponseHeaders(theRequestDetails, outcome);

    // Add correct headers
    HttpServletResponse response = theRequestDetails.getServletResponse();
    response.setHeader("Accept", "application/json");
    response.setHeader("Content-Type", "application/json");
  }

  private boolean shouldUseCache(ServletRequestDetails theRequestDetails) {
    CacheControlDirective cacheControlDirective = new CacheControlDirective()
        .parse(theRequestDetails.getHeaders(Constants.HEADER_CACHE_CONTROL));
    return !cacheControlDirective.isNoCache();
  }

  private String getServerBase(ServletRequestDetails theRequestDetails) {
    return StringUtils.removeEnd(theRequestDetails.getServerBaseForRequest(), "/");
  }

  /**
   * Group/Id/$export function.
   */
  @Operation(name = JpaConstants.OPERATION_EXPORT, manualResponse = true, idempotent = true,
      typeName = "Group")
  public void groupExport(@IdParam IIdType theIdParam,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_OUTPUT_FORMAT, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theOutputFormat,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_TYPE, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theType,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_SINCE, min = 0, max = 1,
          typeName = "instant") IPrimitiveType<Date> theSince,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_TYPE_FILTER, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theTypeFilter,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_MDM, min = 0, max = 1,
          typeName = "boolean") IPrimitiveType<Boolean> theMdm,
      ServletRequestDetails theRequestDetails) {

    ourLog.debug("Received Group Bulk Export Request for Group {}", theIdParam);
    ourLog.debug("_type={}", theIdParam);
    ourLog.debug("_since={}", theSince);
    ourLog.debug("_typeFilter={}", theTypeFilter);
    ourLog.debug("_mdm=", theMdm);

    try {
      authorize(theRequestDetails);

      validatePreferAsyncHeader(theRequestDetails);
    }

    catch (InvalidRequestException invalidRequestException) {
      handleInvalidRequestException(invalidRequestException, theRequestDetails);
      return;
    }

    BulkDataExportOptions bulkDataExportOptions = buildGroupBulkExportOptions(theOutputFormat,
        theType, theSince, theTypeFilter, theIdParam, theMdm);
    validateResourceTypesAllContainPatientSearchParams(bulkDataExportOptions.getResourceTypes());

    // currently default is only Patient, want ALL
    // https://github.com/hapifhir/hapi-fhir/blob/dc627dc019d063aec6f651c0470b8c30d89db882/hapi-fhir-jpaserver-base/src/main/java/ca/uhn/fhir/jpa/bulk/export/svc/BulkDataExportSvcImpl.java#L383
    if (bulkDataExportOptions.getResourceTypes() == null) {

      Set<String> resourceTypes = getDefaultResourceTypes();
      bulkDataExportOptions.setResourceTypes(resourceTypes);
    }


    IBulkDataExportSvc.JobInfo outcome =
        myBulkDataExportSvc.submitJob(bulkDataExportOptions, shouldUseCache(theRequestDetails));
    writePollingLocationToResponseHeaders(theRequestDetails, outcome);

    // Add correct headers
    HttpServletResponse response = theRequestDetails.getServletResponse();
    response.setHeader(Constants.HEADER_ACCEPT, "application/json");
    response.addHeader(Constants.HEADER_CONTENT_TYPE, "application/json");
  }

  private Set<String> getDefaultResourceTypes() {
    Set<String> resourceTypes = new HashSet<String>();
    resourceTypes.addAll(Arrays.asList(DEFAULT_RESOURCE_TYPES));
    return resourceTypes;
  }

  private void validateResourceTypesAllContainPatientSearchParams(Set<String> theResourceTypes) {
    if (theResourceTypes != null) {
      List<String> badResourceTypes = theResourceTypes.stream()
          .filter(resourceType -> !myBulkDataExportSvc.getPatientCompartmentResources()
              .contains(resourceType))
          .collect(Collectors.toList());

      if (!badResourceTypes.isEmpty()) {
        throw new InvalidRequestException(String.format(
            "Resource types [%s] are invalid for this type of export, as they do not contain search parameters that refer to patients.",
            String.join(",", badResourceTypes)));
      }
    }
  }

  /**
   * Patient/$export function.
   */
  @Operation(name = JpaConstants.OPERATION_EXPORT, manualResponse = true, idempotent = true,
      typeName = "Patient")
  public void patientExport(
      @OperationParam(name = JpaConstants.PARAM_EXPORT_OUTPUT_FORMAT, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theOutputFormat,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_TYPE, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theType,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_SINCE, min = 0, max = 1,
          typeName = "instant") IPrimitiveType<Date> theSince,
      @OperationParam(name = JpaConstants.PARAM_EXPORT_TYPE_FILTER, min = 0, max = 1,
          typeName = "string") IPrimitiveType<String> theTypeFilter,
      ServletRequestDetails theRequestDetails) {

    try {
      authorize(theRequestDetails);

      validatePreferAsyncHeader(theRequestDetails);
    }

    catch (InvalidRequestException invalidRequestException) {
      handleInvalidRequestException(invalidRequestException, theRequestDetails);
      return;
    }
    BulkDataExportOptions bulkDataExportOptions =
        buildPatientBulkExportOptions(theOutputFormat, theType, theSince, theTypeFilter);
    validateResourceTypesAllContainPatientSearchParams(bulkDataExportOptions.getResourceTypes());
    IBulkDataExportSvc.JobInfo outcome =
        myBulkDataExportSvc.submitJob(bulkDataExportOptions, shouldUseCache(theRequestDetails));
    writePollingLocationToResponseHeaders(theRequestDetails, outcome);

    // Add correct headers
    HttpServletResponse response = theRequestDetails.getServletResponse();
    response.setHeader("Accept", "application/json");
    response.setHeader("Content-Type", "application/json");
  }

  /**
   * $export-poll-status function.
   */
  @Operation(name = JpaConstants.OPERATION_EXPORT_POLL_STATUS, manualResponse = true,
      idempotent = true, manualRequest = true)
  public void exportPollStatus(
      @OperationParam(name = JpaConstants.PARAM_EXPORT_POLL_STATUS_JOB_ID, typeName = "string",
          min = 0, max = 1) IPrimitiveType<String> theJobId,
      ServletRequestDetails theRequestDetails) throws IOException {

    HttpServletResponse response = theRequestDetails.getServletResponse();
    theRequestDetails.getServer().addHeadersToResponse(response);

    IBulkDataExportSvc.JobInfo status =
        myBulkDataExportSvc.getJobInfoOrThrowResourceNotFound(theJobId.getValueAsString());

    switch (status.getStatus()) {
      case SUBMITTED:
      case BUILDING:

        response.setStatus(Constants.STATUS_HTTP_202_ACCEPTED);
        response.addHeader(Constants.HEADER_X_PROGRESS,
            "Build in progress - Status set to " + status.getStatus() + " at "
                + new InstantType(status.getStatusTime()).getValueAsString());
        response.addHeader(Constants.HEADER_RETRY_AFTER, "120");
        break;

      case COMPLETE:

        response.setStatus(Constants.STATUS_HTTP_200_OK);
        response.setContentType(Constants.CT_JSON);

        // Create a JSON response
        BulkExportResponseJson bulkResponseDocument = new BulkExportResponseJson();
        bulkResponseDocument.setTransactionTime(status.getStatusTime());
        bulkResponseDocument.setRequest(status.getRequest());
        bulkResponseDocument.setRequiresAccessToken(true);

        for (IBulkDataExportSvc.FileEntry nextFile : status.getFiles()) {
          String serverBase = getServerBase(theRequestDetails);


          String nextUrl =
              serverBase + "/" + nextFile.getResourceId().toUnqualifiedVersionless().getValue();


          bulkResponseDocument.addOutput().setType(nextFile.getResourceType()).setUrl(nextUrl);
        }

        // UGLY FIX: call the getter which will make output an empty array instead of null for
        // purposes of writing to json
        bulkResponseDocument.getOutput();
        bulkResponseDocument.getError();

        JsonUtil.serialize(bulkResponseDocument, response.getWriter());
        response.getWriter().close();
        break;

      case ERROR:

        response.setStatus(Constants.STATUS_HTTP_500_INTERNAL_ERROR);
        response.setContentType(Constants.CT_FHIR_JSON);

        // Create an OperationOutcome response
        IBaseOperationOutcome oo = OperationOutcomeUtil.newInstance(myFhirContext);
        OperationOutcomeUtil.addIssue(myFhirContext, oo, "error", status.getStatusMessage(), null,
            null);
        myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(oo,
            response.getWriter());
        response.getWriter().close();
    }
  }

  // Bulk Interceptor will redirect here from a DELETE on $export-poll-status REQUEST
  @Operation(name = "$bulk-delete", manualResponse = true, idempotent = false, manualRequest = true)
  public void bulkDelete(

      @OperationParam(name = JpaConstants.PARAM_EXPORT_POLL_STATUS_JOB_ID, typeName = "string",
          min = 0, max = 1) IPrimitiveType<String> theJobId,
      ServletRequestDetails theRequestDetails) throws IOException {

    try {
      HttpServletResponse response = theRequestDetails.getServletResponse();
      theRequestDetails.getServer().addHeadersToResponse(response);

      response.setStatus(Constants.STATUS_HTTP_202_ACCEPTED);
      response.getWriter().close();
    }

    catch (InvalidRequestException invalidRequestException) {
      handleInvalidRequestException(invalidRequestException, theRequestDetails);
    }

  }

  private BulkDataExportOptions buildSystemBulkExportOptions(IPrimitiveType<String> theOutputFormat,
      IPrimitiveType<String> theType, IPrimitiveType<Date> theSince,
      IPrimitiveType<String> theTypeFilter) {
    return buildBulkDataExportOptions(theOutputFormat, theType, theSince, theTypeFilter,
        BulkDataExportOptions.ExportStyle.SYSTEM);
  }

  private BulkDataExportOptions buildGroupBulkExportOptions(IPrimitiveType<String> theOutputFormat,
      IPrimitiveType<String> theType, IPrimitiveType<Date> theSince,
      IPrimitiveType<String> theTypeFilter, IIdType theGroupId,
      IPrimitiveType<Boolean> theExpandMdm) {
    BulkDataExportOptions bulkDataExportOptions = buildBulkDataExportOptions(theOutputFormat,
        theType, theSince, theTypeFilter, BulkDataExportOptions.ExportStyle.GROUP);
    bulkDataExportOptions.setGroupId(theGroupId);

    boolean mdm = false;
    if (theExpandMdm != null) {
      mdm = theExpandMdm.getValue();
    }
    bulkDataExportOptions.setExpandMdm(mdm);

    return bulkDataExportOptions;
  }

  private BulkDataExportOptions buildPatientBulkExportOptions(
      IPrimitiveType<String> theOutputFormat, IPrimitiveType<String> theType,
      IPrimitiveType<Date> theSince, IPrimitiveType<String> theTypeFilter) {
    return buildBulkDataExportOptions(theOutputFormat, theType, theSince, theTypeFilter,
        BulkDataExportOptions.ExportStyle.PATIENT);
  }

  private BulkDataExportOptions buildBulkDataExportOptions(IPrimitiveType<String> theOutputFormat,
      IPrimitiveType<String> theType, IPrimitiveType<Date> theSince,
      IPrimitiveType<String> theTypeFilter, BulkDataExportOptions.ExportStyle theExportStyle) {
    String outputFormat = theOutputFormat != null ? theOutputFormat.getValueAsString() : null;

    Set<String> resourceTypes = null;
    if (theType != null) {
      resourceTypes = ArrayUtil.commaSeparatedListToCleanSet(theType.getValueAsString());
    }

    Date since = null;
    if (theSince != null) {
      since = theSince.getValue();
    }

    Set<String> typeFilters = splitTypeFilters(theTypeFilter);

    BulkDataExportOptions bulkDataExportOptions = new BulkDataExportOptions();
    bulkDataExportOptions.setFilters(typeFilters);
    bulkDataExportOptions.setExportStyle(theExportStyle);
    bulkDataExportOptions.setSince(since);
    bulkDataExportOptions.setResourceTypes(resourceTypes);
    bulkDataExportOptions.setOutputFormat(outputFormat);
    return bulkDataExportOptions;
  }

  public void writePollingLocationToResponseHeaders(ServletRequestDetails theRequestDetails,
      IBulkDataExportSvc.JobInfo theOutcome) {
    String serverBase = getServerBase(theRequestDetails);
    String pollLocation = serverBase + "/" + JpaConstants.OPERATION_EXPORT_POLL_STATUS + "?"
        + JpaConstants.PARAM_EXPORT_POLL_STATUS_JOB_ID + "=" + theOutcome.getJobId();

    HttpServletResponse response = theRequestDetails.getServletResponse();

    // Add standard headers
    theRequestDetails.getServer().addHeadersToResponse(response);

    // Successful 202 Accepted
    response.addHeader(Constants.HEADER_CONTENT_LOCATION, pollLocation);
    response.setStatus(Constants.STATUS_HTTP_202_ACCEPTED);
  }

  private void validatePreferAsyncHeader(ServletRequestDetails theRequestDetails)
      throws InvalidRequestException {
    String preferHeader = theRequestDetails.getHeader(Constants.HEADER_PREFER);
    PreferHeader prefer = RestfulServerUtils.parsePreferHeader(null, preferHeader);
    if (prefer.getRespondAsync() == false) {
      throw new InvalidRequestException("Must request async processing for $export");
    }
  }

  private Set<String> splitTypeFilters(IPrimitiveType<String> theTypeFilter) {
    if (theTypeFilter == null) {
      return null;
    }
    String typeFilterSring = theTypeFilter.getValueAsString();
    String[] typeFilters = typeFilterSring.split(FARM_TO_TABLE_TYPE_FILTER_REGEX);
    if (typeFilters == null || typeFilters.length == 0) {
      return null;
    }

    return new HashSet<>(Arrays.asList(typeFilters));
  }


  // checks authorization and will throw Runtime Exceptions
  private void authorize(ServletRequestDetails theRequestDetails) throws InvalidRequestException {
    // confirm that header Accept value exists and is application/fhir+json
    String acceptHeader = theRequestDetails.getHeader(ACCEPT);
    if (!VALID_ACCEPT_HEADERS.contains(acceptHeader)) {
      String message = "Accept header was not provided or was invalid";

      throw new InvalidRequestException(message);
    }
  }

  // UGLY SIDE EFFECT FIX, For now, there is a bug where Exceptions Content-Type get overridden , so
  // instead of throwing exception, just change the response. Call this method in a catch block for
  // InvalidRequestException
  private void handleInvalidRequestException(InvalidRequestException invalidRequestException,
      ServletRequestDetails theRequestDetails) {
    // Add correct headers
    HttpServletResponse response = theRequestDetails.getServletResponse();
    response.setContentType("application/json");

    response.setStatus(400);

    String message = invalidRequestException.getMessage();
    OperationOutcome operationOutcome = new OperationOutcome();
    List<OperationOutcomeIssueComponent> issues = new ArrayList<OperationOutcomeIssueComponent>();
    OperationOutcomeIssueComponent issue = new OperationOutcomeIssueComponent();
    issue.setDiagnostics(message);
    issue.setCode(IssueType.PROCESSING);
    issue.setSeverity(IssueSeverity.ERROR);
    issues.add(issue);
    operationOutcome.setIssue(issues);

    try {
      myFhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToWriter(operationOutcome,
          response.getWriter());
      response.getWriter().close();

    } catch (DataFormatException | IOException e) {
      e.printStackTrace();
    }

  }
}
