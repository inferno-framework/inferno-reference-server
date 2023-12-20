package org.mitre.fhir;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.config.DaoConfig;
import ca.uhn.fhir.jpa.api.config.DaoConfig.ClientIdStrategyEnum;
import ca.uhn.fhir.jpa.api.config.DaoConfig.IdStrategyEnum;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.TerminologyUploaderProvider;
import ca.uhn.fhir.jpa.provider.r4.JpaSystemProviderR4;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import javax.servlet.ServletException;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.mitre.fhir.authorization.FakeOauth2AuthorizationInterceptorAdaptor;
import org.mitre.fhir.authorization.ServerConformanceWithAuthorizationProvider;
import org.mitre.fhir.bulk.AuthorizationBulkDataExportProvider;
import org.mitre.fhir.bulk.BulkInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

/**
 * MitreJpaServer configures the server.
 *
 * @author Tim Shaffer
 */

public class MitreJpaServer extends RestfulServer {
  private static final long serialVersionUID = 1L;
  private static final String READ_ONLY_ENV_KEY = "READ_ONLY";
  private static final String FORCE_LOAD_RESOURCES_ENV_KEY = "FORCE_LOAD_RESOURCES";


  @Autowired
  protected ISearchParamRegistry searchParamRegistry;

  @Autowired
  AuthorizationBulkDataExportProvider authorizationBulkDataExportProvider;

  public MitreJpaServer() {
    // Required for Autowiring searchParamRegistry
    SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
  }

  @Override
  protected void initialize() throws ServletException {
    super.initialize();

    // Load ReadOnly var
    String readOnly = System.getenv().get(READ_ONLY_ENV_KEY);
    if (readOnly == null) {
      readOnly = System.getProperty(READ_ONLY_ENV_KEY);
    }

    // Setup a FHIR context.
    FhirVersionEnum fhirVersion = FhirVersionEnum.R4;
    setFhirContext(new FhirContext(fhirVersion));

    // Get the Spring context from the web container (it's declared in web.xml)
    ApplicationContext appContext = (ApplicationContext) getServletContext()
        .getAttribute("org.springframework.web.context.WebApplicationContext.ROOT");

    DaoConfig daoConfig = appContext.getBean(DaoConfig.class);
    // Auto-create placeholder reference targets to allow loading resources in any order
    daoConfig.setAutoCreatePlaceholderReferenceTargets(true);
    // Allow "clients" to use any Id strategy, ie, allow loading resources with numeric IDs
    daoConfig.setResourceClientIdStrategy(ClientIdStrategyEnum.ANY);
    // POSTed resources with no IDs will be assigned UUIDs, to ensure there is no conflict
    // with loaded resources. See doc on ClientIdStrategyEnum.ANY above
    daoConfig.setResourceServerIdStrategy(IdStrategyEnum.UUID);

    // myResourceProvidersR4 is generated as a part of hapi-fhir-jpaserver-base.
    // It contains bean definitions for a resource provider for each resource type.
    ResourceProviderFactory resourceProviders =
        appContext.getBean("myResourceProvidersR4", ResourceProviderFactory.class);
    registerProviders(resourceProviders.createProviders());

    // mySystemProviderR4 is generated as a part of hapi-fhir-jpaserver-base.
    // The system provider implements non-resource-type methods,
    // such as transaction, and global history.
    JpaSystemProviderR4 systemProvider =
        appContext.getBean("mySystemProviderR4", JpaSystemProviderR4.class);
    registerProvider(systemProvider);

    // mySystemDaoR4 is generated as a part of hapi-fhir-jpaserver-base.
    // The conformance provider exports the supported resources,
    // search parameters, etc for this server.
    // The JPA version adds resource counts to the exported statement, so it is a nice addition.
    @SuppressWarnings("unchecked")
    IFhirSystemDao<Bundle, Meta> systemDao =
        appContext.getBean("mySystemDaoR4", IFhirSystemDao.class);

    ServerConformanceWithAuthorizationProvider confProvider =
        new ServerConformanceWithAuthorizationProvider(this, systemDao,
            appContext.getBean(DaoConfig.class), searchParamRegistry);
    confProvider.setImplementationDescription("HAPI FHIR R4 Server");
    setServerConformanceProvider(confProvider);

    // Enable e-tag support.
    setETagSupport(ETagSupportEnum.ENABLED);

    // Default to JSON and pretty printing
    setDefaultPrettyPrint(true);
    setDefaultResponseEncoding(EncodingEnum.JSON);

    // This configures the server to page search results to
    // and from the database, instead of only to memory.
    // This may mean a performance hit when performing searches that return lots of results,
    // but makes the server much more scalable.
    setPagingProvider(appContext.getBean(DatabaseBackedPagingProvider.class));

    // If you are using DSTU3+, you may want to add a terminology uploader.
    // This allows uploading of external terminologies such as Snomed CT.
    // It does not have any security attached (any anonymous user may use it by default).
    // Consider using an AuthorizationInterceptor with this feature.
    registerProvider(appContext.getBean(TerminologyUploaderProvider.class));

    // Add logging interceptor.
    LoggingInterceptor loggingInterceptor = new LoggingInterceptor();
    loggingInterceptor.setLoggerName("fhir.access");
    loggingInterceptor
        .setMessageFormat("Path[${servletPath}] " + "Source[${requestHeader.x-forwarded-for}] "
            + "Operation[${operationType} ${operationName} ${idOrResourceName}] "
            + "UA[${requestHeader.user-agent}] " + "Params[${requestParameters}] "
            + "ResponseEncoding[${responseEncodingNoDefault}]");
    registerInterceptor(loggingInterceptor);

    registerInterceptor(new BulkInterceptor());

    registerInterceptor(new FakeOauth2AuthorizationInterceptorAdaptor());

    if (readOnly == null || Boolean.parseBoolean(readOnly)) {
      registerInterceptor(new ReadOnlyInterceptor());
    }

    // enable Bulk Export
    registerProvider(authorizationBulkDataExportProvider);

    try {
      URI resourcesURI = MitreJpaServer.class.getClassLoader().getResource("fhir_resources").toURI();
      Path fhirResources = Paths.get(resourcesURI);

      loadResources(appContext, fhirResources);
    } catch (Exception e) {
      throw new ServletException("Error in loading resources from file", e);
    }
  }

  private void loadResources(ApplicationContext appContext, Path fhirResources) throws Exception {
    DaoRegistry registry = new DaoRegistry(getFhirContext());
    registry.setApplicationContext(appContext);

    Map<String, Long> resCounts = registry.getSystemDao().getResourceCounts();

    if (resCounts.isEmpty() || resCounts.getOrDefault("Patient", 0L) == 0) {
      System.out.println("Server is empty. Loading resources from local files.");
    } else if (Boolean.parseBoolean(System.getenv(FORCE_LOAD_RESOURCES_ENV_KEY))) {
      System.out.println("Loading resources from local files since FORCE_LOAD_RESOURCES is set.");
    } else {
      System.out.println("Server not empty, skipping loading resources from local files.");
      return;
    }

    Files.walk(fhirResources, Integer.MAX_VALUE)
        .filter(Files::isReadable)
        .filter(Files::isRegularFile)
        .filter(p -> p.toString().endsWith(".json"))
        .forEach(p -> {
          try {
            File file = p.toFile();
            System.out.println("Loading " + file.getName());
            Resource resource = new JsonParser().parse(FileUtils.readFileToByteArray(file));

            if (resource instanceof Bundle) {
              registry.getSystemDao().transaction(null, resource);
            } else {
              String resourceType = resource.getResourceType().toString();

              // IMPORTANT: the HAPI parser appends version numbers to this ID when parsing from file,
              // but not when parsing from the body of an HTTP request, even when the content of both
              // is exactly the same.
              // That version number causes pain here, so remove it.
              resource.setId(resourceType + "/" + resource.getIdElement().getIdPart());

              registry.getResourceDao(resource.fhirType()).update(resource);
            }
          } catch ( Exception e ) {
            System.out.println("Unable to load " + p.toString());
            e.printStackTrace();
          }
        });

    System.out.println("Done loading resources.");
  }
}
