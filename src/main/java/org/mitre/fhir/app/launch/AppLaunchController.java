package org.mitre.fhir.app.launch;

import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;
import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppLaunchController {

  /**
   * Provides the base url of the FHIR Server.
   *
   * @param request the incoming HTTP Request
   * @return String the base url of the FHIR Server
   */
  @GetMapping(path = "/fhir-server-path")
  public ResponseEntity<String> getFhirServerPath(HttpServletRequest request) {
    String fhirServerBaseUrl = FhirReferenceServerUtils.getFhirServerBaseUrl(request);
    ResponseEntity<String> responseEntity = new ResponseEntity<String>(fhirServerBaseUrl,
        HttpStatus.OK);
    return responseEntity;
  }

  /**
   * Provides the SMART App Styling
   * http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/#styling
   *
   * @param theRequest the incoming HTTP Request
   * @return String the JSON SMART Styling response
   */
  @GetMapping(path = "/smart-style-url", produces = {"application/json"})
  public String getSmartStyleUrl(HttpServletRequest theRequest) {
    //example json from http://www.hl7.org/fhir/smart-app-launch/scopes-and-launch-context/index.html#styling
    JSONObject json = new JSONObject();
    json.put("color_background", "#edeae3");
    json.put("color_error", "#9e2d2d");
    json.put("color_highlight", "#69b5ce");
    json.put("color_modal_backdrop", "");
    json.put("color_success", "#498e49");
    json.put("color_text", "#303030");
    json.put("dim_border_radius", "6px");
    json.put("dim_font_size", "13px");
    json.put("dim_spacing_size", "20px");
    json.put("font_family_body", "Georgia, Times, 'Times New Roman', serif");
    json.put("font_family_heading",
        "'HelveticaNeue-Light', Helvetica, Arial, 'Lucida Grande', sans-serif;");
    return json.toString();
  }
}
