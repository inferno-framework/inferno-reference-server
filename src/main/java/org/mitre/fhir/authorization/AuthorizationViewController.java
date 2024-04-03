package org.mitre.fhir.authorization;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.UriComponentsBuilder;

/*
 * Controller that serves the src/main/webapp/WEB-INF/templates/authorization.html template
 */
@Controller
public class AuthorizationViewController {

  @GetMapping("/authorization")
  public String showGetAuthorizationView(HttpServletRequest request) {
    return "authorization"; // String will be mapped to corresponding template html file
  }

  /**
   * Redirects POST requests to @GetMapping above.
   */
  @PostMapping("/authorization")
  public String showPostAuthorizationView(HttpServletRequest request,
                                          @RequestBody MultiValueMap<String, String> payload) {
    String redirect = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString())
          .queryParams(payload).build().toUriString();
    return "redirect:" + redirect;
  }
  
  @RequestMapping("/patient-picker")
  public String showPatientPickerView() {
    return "patient-picker"; // String will be mapped to corresponding template html file
  }

}
