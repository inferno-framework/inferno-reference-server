package org.mitre.fhir.landing;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/*
 * Controller that serves the src/main/webapp/WEB-INF/templates/landing.html template
 */
@Controller
public class LandingViewController {
  @RequestMapping("/reference-server")
  public String showLandingView() {
    return "landing"; // String will be mapped to corresponding template html file
  }
  
  @RequestMapping("")
  public void showDefaultView() {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find resource");

  }

}
