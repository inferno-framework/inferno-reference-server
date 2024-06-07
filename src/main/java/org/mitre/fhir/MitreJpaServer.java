package org.mitre.fhir;

import ca.uhn.fhir.batch2.jobs.export.BulkDataExportProvider;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.dao.DaoRegistry;
import ca.uhn.fhir.jpa.api.dao.IFhirSystemDao;
import ca.uhn.fhir.jpa.provider.JpaSystemProvider;
import ca.uhn.fhir.jpa.provider.TerminologyUploaderProvider;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.ETagSupportEnum;
import ca.uhn.fhir.rest.server.RestfulServer;
import ca.uhn.fhir.rest.server.interceptor.LoggingInterceptor;
import ca.uhn.fhir.rest.server.provider.ResourceProviderFactory;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import jakarta.servlet.ServletException;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.hl7.fhir.r4.formats.JsonParser;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Resource;
import org.mitre.fhir.authorization.FakeOauth2AuthorizationInterceptorAdaptor;
import org.mitre.fhir.authorization.ServerConformanceWithAuthorizationProvider;
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
  BulkDataExportProvider bulkDataExportProvider;

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

    // myResourceProvidersR4 is generated as a part of hapi-fhir-jpaserver-base.
    // It contains bean definitions for a resource provider for each resource type.
    ResourceProviderFactory resourceProviders =
        appContext.getBean("myResourceProvidersR4", ResourceProviderFactory.class);
    registerProviders(resourceProviders.createProviders());

    // mySystemProviderR4 is generated as a part of hapi-fhir-jpaserver-base.
    // The system provider implements non-resource-type methods,
    // such as transaction, and global history.
    JpaSystemProvider systemProvider =
        appContext.getBean("mySystemProviderR4", JpaSystemProvider.class);
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
            appContext.getBean(JpaStorageSettings.class), searchParamRegistry);
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

    registerInterceptor(new BulkInterceptor(this.getFhirContext()));

    DaoRegistry registry = new DaoRegistry(getFhirContext());
    registry.setApplicationContext(appContext);

    
    registerInterceptor(new FakeOauth2AuthorizationInterceptorAdaptor(registry));

    if (readOnly == null || Boolean.parseBoolean(readOnly)) {
      registerInterceptor(new ReadOnlyInterceptor());
    }

    // enable Bulk Export
    registerProvider(bulkDataExportProvider);

    try {
      String resourcesFolder = new HapiReferenceServerProperties().getResourcesFolder();
      Path fhirResources = Paths.get(resourcesFolder);
      loadResources(registry, fhirResources);
    } catch (Exception e) {
      throw new ServletException("Error in loading resources from file", e);
    }
  }

  private void loadResources(DaoRegistry registry, Path fhirResources) throws Exception {

    Map<String, Long> resCounts = registry.getSystemDao().getResourceCounts();

    if (resCounts.isEmpty() || resCounts.getOrDefault("Patient", 0L) == 0) {
      System.out.println("Server is empty. Loading resources from local files.");
    } else if (Boolean.parseBoolean(System.getenv(FORCE_LOAD_RESOURCES_ENV_KEY))) {
      System.out.println("Loading resources from local files since FORCE_LOAD_RESOURCES is set.");
    } else {
      System.out.println("Server not empty, skipping loading resources from local files.");
      return;
    }

    File dir = fhirResources.toFile();
    File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
    if (files == null || files.length == 0) {
      System.out.println("No resources to load.");
      return;
    }
    Arrays.sort(files); // sort files for consistent and predictable ordering
    for (File file : files) {
      try {
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

          registry.getResourceDao(resource.fhirType()).update(resource, (RequestDetails) null);
        }
      } catch (Exception e) {
        System.out.println("Unable to load " + file.getName());
        e.printStackTrace();
      }
    }
    System.out.println("Done loading resources.");
  }
}
