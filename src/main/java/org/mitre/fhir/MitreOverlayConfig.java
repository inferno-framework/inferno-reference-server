package org.mitre.fhir;

import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.to.FhirTesterMvcConfig;
import ca.uhn.fhir.to.TesterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Server Configuration.
 * @author Tim Shaffer
 */
@Configuration
@Import(FhirTesterMvcConfig.class)
@ComponentScan(basePackages = {"org.mitre.fhir.app.launch"})
public class MitreOverlayConfig {

  /**
   * Returns the testing configuration.
   * @return the testing config
   */
  @Bean
  public TesterConfig testerConfig() {
    TesterConfig config = new TesterConfig();
    config.addServer()
        .withId("home")
        .withFhirVersion(FhirVersionEnum.R4)
        .withBaseUrl("${serverBase}/r4")
        .withName("MITRE R4");
    return config;
  }

}
