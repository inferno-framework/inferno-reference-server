package org.mitre.fhir.app.launch;

import javax.servlet.http.HttpServletRequest;

import org.mitre.fhir.utils.FhirReferenceServerUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AppLaunchController {

	@GetMapping(path = "/fhir-server-path")
	public ResponseEntity<String> getFhirServerPath(HttpServletRequest request)
	{
		String fhirServerBaseUrl = FhirReferenceServerUtils.getFhirServerBaseUrl(request);
		ResponseEntity<String> responseEntity = new ResponseEntity<String>(fhirServerBaseUrl, HttpStatus.OK);
		return responseEntity;
	}
}
