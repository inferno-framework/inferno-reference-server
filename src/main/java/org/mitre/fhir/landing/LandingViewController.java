package org.mitre.fhir.landing;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/*
 * Controller that serves the src/main/webapp/WEB-INF/templates/landing.html template
 */
@Controller
public class LandingViewController {
  @RequestMapping("/reference-server")
  public String showLandingView() {
    return "landing"; // String will be mapped to corresponding template html file
  }

}
