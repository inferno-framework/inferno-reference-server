package org.mitre.fhir.landing;

import org.mitre.fhir.HapiReferenceServerProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

/*
 * Controller that serves the src/main/webapp/WEB-INF/templates/landing.html template
 */
@Controller
public class LandingViewController {

  /**
   * Returns landing html template.
   * 
   * @param model the model that will be used by the template.
   * @return html template name.
   */
  @RequestMapping("/reference-server")
  public String showLandingView(Model model) {

    HapiReferenceServerProperties properties = new HapiReferenceServerProperties();
    model.addAttribute("publicClientId", properties.getPublicClientId());
    model.addAttribute("confidentialClientId", properties.getConfidentialClientId());
    model.addAttribute("confidentialClientSecret", properties.getConfidentialClientSecret());
    model.addAttribute("asymmetricClientId", properties.getAsymmetricClientId());
    model.addAttribute("bulkClientId", properties.getBulkClientId());
    model.addAttribute("groupId", properties.getGroupId());


    return "landing"; // String will be mapped to corresponding template html file
  }

  @RequestMapping("")
  public void showDefaultView() {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Unable to find resource");

  }

}
