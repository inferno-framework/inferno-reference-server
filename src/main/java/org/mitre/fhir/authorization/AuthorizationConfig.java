package org.mitre.fhir.authorization;

import ca.uhn.fhir.to.TesterConfig;
import org.mitre.fhir.InfernoReferenceServerWebConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

//simplified copy of config from  ca.uhn.fhir.jpa.starter.FhirTesterConfig
@Import(InfernoReferenceServerWebConfig.class)
@ComponentScan(basePackages = {"org.mitre.fhir.authorization"})
public class AuthorizationConfig {

  @Bean
  public TesterConfig testerConfig() {
    TesterConfig retVal = new TesterConfig();
    return retVal;
  }
}
