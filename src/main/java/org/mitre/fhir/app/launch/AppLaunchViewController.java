package org.mitre.fhir.app.launch;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/*
 * Controller that serves the src/main/webapp/WEB-INF/templates/app-launch.html template
 */
@Controller
public class AppLaunchViewController {

  @RequestMapping("/app-launch")
  public String showAppLaunchView() {
    return "app-launch"; // String will be mapped to corresponding template html file
  }

}
