package org.mitre.fhir.authorization;

import org.apache.commons.collections4.MultiValuedMap;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/*
 * Controller that serves the src/main/webapp/WEB-INF/templates/authorization.html template
 */
@Controller
public class AuthorizationViewController {

  @GetMapping("/authorization")
  public String showGetAuthorizationView(HttpServletRequest request) {
    return "authorization"; // String will be mapped to corresponding template html file
  }

  @PostMapping("/authorization")
  public String showPostAuthorizationView(HttpServletRequest request, @RequestBody MultiValueMap<String, String> payload) {
    String redirect = UriComponentsBuilder.fromHttpUrl(request.getRequestURL().toString()).queryParams(payload).build().toUriString();
    return "redirect:" + redirect;
  }
  
  @RequestMapping("/patient-picker")
  public String showPatientPickerView() {
    return "patient-picker"; // String will be mapped to corresponding template html file
  }

}
