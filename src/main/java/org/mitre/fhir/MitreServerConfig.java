package org.mitre.fhir;

import ca.uhn.fhir.batch2.jobs.config.Batch2JobsConfig;
import ca.uhn.fhir.context.ConfigurationException;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.jpa.api.IDaoRegistry;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings.ClientIdStrategyEnum;
import ca.uhn.fhir.jpa.api.config.JpaStorageSettings.IdStrategyEnum;
import ca.uhn.fhir.jpa.api.config.ThreadPoolFactoryConfig;
import ca.uhn.fhir.jpa.batch2.JpaBatch2Config;
import ca.uhn.fhir.jpa.config.r4.JpaR4Config;
import ca.uhn.fhir.jpa.config.util.HapiEntityManagerFactoryUtil;
import ca.uhn.fhir.jpa.config.util.ValidationSupportConfigUtil;
import ca.uhn.fhir.jpa.dao.DaoSearchParamProvider;
import ca.uhn.fhir.jpa.model.config.PartitionSettings;
import ca.uhn.fhir.jpa.model.entity.StorageSettings.IndexEnabledEnum;
import ca.uhn.fhir.jpa.provider.DaoRegistryResourceSupportedSvc;
import ca.uhn.fhir.jpa.search.DatabaseBackedPagingProvider;
import ca.uhn.fhir.jpa.searchparam.registry.ISearchParamProvider;
import ca.uhn.fhir.jpa.searchparam.registry.SearchParamRegistryImpl;
import ca.uhn.fhir.jpa.subscription.channel.config.SubscriptionChannelConfig;
import ca.uhn.fhir.jpa.subscription.match.config.SubscriptionProcessorConfig;
import ca.uhn.fhir.jpa.subscription.match.config.WebsocketDispatcherConfig;
import ca.uhn.fhir.jpa.subscription.match.deliver.email.IEmailSender;
import ca.uhn.fhir.jpa.subscription.submit.config.SubscriptionSubmitterConfig;
import ca.uhn.fhir.jpa.validation.JpaValidationSupportChain;
import ca.uhn.fhir.rest.api.IResourceSupportedSvc;
import ca.uhn.fhir.rest.server.interceptor.CorsInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import ca.uhn.fhir.rest.server.util.ISearchParamRegistry;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Driver;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import javax.sql.DataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.hl7.fhir.common.hapi.validation.support.CachingValidationSupport;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.cors.CorsConfiguration;

/**
 * Configures the Server and Database.
 * 
 * @author Tim Shaffer
 */
@Configuration
@EnableTransactionManagement
@Import({JpaR4Config.class, Batch2JobsConfig.class, JpaBatch2Config.class,
    SubscriptionSubmitterConfig.class, SubscriptionProcessorConfig.class,
    SubscriptionChannelConfig.class, WebsocketDispatcherConfig.class,
    ThreadPoolFactoryConfig.class})
public class MitreServerConfig {

  /**
   * Returns the Data Access Object (DAO) configuration.
   * 
   * @return the DAO configuration.
   */
  @Bean
  public JpaStorageSettings daoConfig() {
    JpaStorageSettings config = new JpaStorageSettings();
    config.setAllowExternalReferences(true);
    config.getTreatBaseUrlsAsLocal().add("http://hl7.org/fhir/us/core/");
    config.setIndexMissingFields(IndexEnabledEnum.ENABLED);

    // Auto-create placeholder reference targets to allow loading resources in any order
    config.setAutoCreatePlaceholderReferenceTargets(true);
    // Allow "clients" to use any Id strategy, ie, allow loading resources with numeric IDs
    config.setResourceClientIdStrategy(ClientIdStrategyEnum.ANY);
    // POSTed resources with no IDs will be assigned UUIDs, to ensure there is no conflict
    // with loaded resources. See doc on ClientIdStrategyEnum.ANY above
    config.setResourceServerIdStrategy(IdStrategyEnum.UUID);
    return config;
  }

  /**
   * Returns the data source for the server.
   * 
   * @return the data source
   */
  @Bean
  public DataSource dataSource() {

    BasicDataSource dataSource = new BasicDataSource();

    try {
      // org.apache.derby.jdbc.EmbeddedDriver
      HapiReferenceServerProperties hapiReferenceServerProperties =
          new HapiReferenceServerProperties();

      String driverName = hapiReferenceServerProperties.getDataSourceDriver();
      Driver driver = (Driver) Class.forName(driverName).getConstructor().newInstance();
      dataSource.setDriver(driver);

      String url = hapiReferenceServerProperties.getDataSourceUrl();
      dataSource.setUrl(url);

      String username = hapiReferenceServerProperties.getDataSourceUsername();
      dataSource.setUsername(username);

      String password = hapiReferenceServerProperties.getDataSourcePassword();
      dataSource.setPassword(password);

      String schema = hapiReferenceServerProperties.getDataSourceSchema();
      if (schema != null) {
        dataSource.setDefaultSchema(schema);
      }
      return dataSource;
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(0);
    }

    return dataSource;
  }

  /**
   * Returns the LocalContainerEntityMangerFactoryBean.
   * 
   * @return the LocalContainerEntityMangerFactoryBean
   */
  @Primary
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory(
      DataSource myDataSource,
      ConfigurableListableBeanFactory myConfigurableListableBeanFactory,
      FhirContext theFhirContext, JpaStorageSettings theStorageSettings) {
    LocalContainerEntityManagerFactoryBean retVal =
        HapiEntityManagerFactoryUtil.newEntityManagerFactory(myConfigurableListableBeanFactory, theFhirContext, theStorageSettings);
    retVal.setPersistenceUnitName("HAPI_PU");

    try {
      retVal.setDataSource(myDataSource);
    } catch (Exception e) {
      throw new ConfigurationException("Could not set the data source due to a configuration issue", e);
    }
    retVal.setJpaProperties(jpaProperties());
    return retVal;
  }

  private Properties jpaProperties() {
    HapiReferenceServerProperties hapiReferenceServerProperties =
        new HapiReferenceServerProperties();

    // https://github.com/hapifhir/hapi-fhir/blob/17d74648debd2d51945b8b7a031d12e3ab16d265/hapi-fhir-jpaserver-base/src/test/java/ca/uhn/fhir/jpa/config/TestR4Config.java#L159
    Properties extraProperties = new Properties();
    extraProperties.put("hibernate.format_sql",
        hapiReferenceServerProperties.getHibernateFormatSql());
    extraProperties.put("hibernate.show_sql", hapiReferenceServerProperties.getHibernateShowSql());
    extraProperties.put("hibernate.hbm2ddl.auto",
        hapiReferenceServerProperties.getHibernateHbdm2ddlAuto());
    extraProperties.put("hibernate.dialect", hapiReferenceServerProperties.getHibernateDialect());
    extraProperties.put("hibernate.search.enabled", "false");

    // lucene hibernate search properties
//    extraProperties.put(BackendSettings.backendKey(BackendSettings.TYPE), "lucene");
//    extraProperties.put(BackendSettings.backendKey(LuceneBackendSettings.ANALYSIS_CONFIGURER),
//        ca.uhn.fhir.jpa.search.HapiHSearchAnalysisConfigurers.HapiLuceneAnalysisConfigurer.class.getName());
//    extraProperties.put(BackendSettings.backendKey(LuceneIndexSettings.DIRECTORY_TYPE),
//        "local-heap");
//    extraProperties.put(BackendSettings.backendKey(LuceneBackendSettings.LUCENE_VERSION),
//        "LUCENE_CURRENT");
//    extraProperties.put(HibernateOrmMapperSettings.ENABLED, "true");

    return extraProperties;
  }
  
  @Bean
  public CorsInterceptor corsInterceptor() {
    // Define your CORS configuration. This is an example
    // showing a typical setup. You should customize this
    // to your specific needs
//    ourLog.info("CORS is enabled on this server");
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedHeader(HttpHeaders.ORIGIN);
    config.addAllowedHeader(HttpHeaders.ACCEPT);
    config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
    config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
    config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
    config.addAllowedHeader("x-fhir-starter");
    config.addAllowedHeader("X-Requested-With");
    config.addAllowedHeader("Prefer");

    List<String> allAllowedCORSOrigins = List.of("*"); //appProperties.getCors().getAllowed_origin();
    allAllowedCORSOrigins.forEach(config::addAllowedOriginPattern);
//    ourLog.info("CORS allows the following origins: " + String.join(", ", allAllowedCORSOrigins));

    config.addExposedHeader("Location");
    config.addExposedHeader("Content-Location");
    config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));
//    config.setAllowCredentials(appProperties.getCors().getAllow_Credentials());

    // Create the interceptor and register it
    return new CorsInterceptor(config);
  }

  @Bean
  public ResponseHighlighterInterceptor responseHighlighterInterceptor() {
    return new ResponseHighlighterInterceptor();
  }

  @Primary
  @Bean
  public CachingValidationSupport validationSupportChain(JpaValidationSupportChain theJpaValidationSupportChain) {
    return ValidationSupportConfigUtil.newCachingValidationSupport(theJpaValidationSupportChain);
  }
  
  /**
   * Returns the JpaTransactionManager.
   * 
   * @param entityManagerFactory the JpaTransactionManager
   * @return the JpaTransactionManager
   */
  @Primary
  @Bean
  public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager retVal = new JpaTransactionManager();
    retVal.setEntityManagerFactory(entityManagerFactory);
    return retVal;
  }

  // Beans that are autowired in other places
  @Bean
  public PartitionSettings partitionSettings() {
    PartitionSettings retVal = new PartitionSettings();
    return retVal;
  }

  @Bean
  public ISearchParamProvider mySearchParamProvider() {
    return new DaoSearchParamProvider();
  }

  // Required for MitreJpaServer
  @Bean
  public ISearchParamRegistry searchParamRegistry() {
    return new SearchParamRegistryImpl();
  }
  
  @Bean
  public IResourceSupportedSvc resourceSupportedSvc(IDaoRegistry theDaoRegistry) {
    return new DaoRegistryResourceSupportedSvc(theDaoRegistry);
  }

  @Bean
  public DatabaseBackedPagingProvider databaseBackedPagingProvider() {
    return new DatabaseBackedPagingProvider();
  }
  
  
  @Bean
  public IEmailSender emailSender() {
    // Return a dummy anonymous function instead of null. Spring does not like null beans.
    return theDetails -> {};
  }
}
