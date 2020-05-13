package org.mitre.fhir.authorization.token;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/*
 * Controller that serves the src/main/webapp/WEB-INF/templates/app-launch.html template
 */
@Controller
@RequestMapping(("/token"))
public class TokenViewController {

  @RequestMapping("/revoke-token")
  public String showRevokeView() {
    return "revoke-token"; // String will be mapped to corresponding template html file
  }

}
