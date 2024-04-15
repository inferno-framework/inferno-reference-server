package org.mitre.fhir.wellknown;

import ca.uhn.fhir.to.TesterConfig;
import org.mitre.fhir.InfernoReferenceServerWebConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

//simplified copy of config from  ca.uhn.fhir.jpa.starter.FhirTesterConfig
@Import(InfernoReferenceServerWebConfig.class)
@ComponentScan(basePackages = {"org.mitre.fhir.wellknown"})
public class WellknownConfig {

}
