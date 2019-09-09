package gov.onc.authorization;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

//@Configuration
@ComponentScan(basePackages={"gov.onc.wellknown"}) //scan for the "wellknown" rest services
public class FhirAuthorizationConfig {

}
