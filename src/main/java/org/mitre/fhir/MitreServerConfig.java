package org.mitre.fhir;

import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.Driver;
import java.util.Properties;

/**
 * @author Tim Shaffer
 */
@Configuration
@EnableTransactionManagement
public class MitreServerConfig extends BaseJavaConfigR4 {

  @Bean
  public DaoConfig daoConfig() {
    DaoConfig config = new DaoConfig();
    config.setAllowExternalReferences(true);
    config.getTreatBaseUrlsAsLocal().add("http://hl7.org/fhir/us/core/");
    return config;
  }

  @Bean
  public ModelConfig modelConfig() {
    return daoConfig().getModelConfig();
  }

  @Bean
  public DataSource dataSource() {

    BasicDataSource dataSource = new BasicDataSource();

    try {
      //org.apache.derby.jdbc.EmbeddedDriver
      HapiReferenceServerProperties hapiReferenceServerProperties = new HapiReferenceServerProperties();

      String driverName = hapiReferenceServerProperties.getDataSourceDriver();
      String url = hapiReferenceServerProperties.getDataSourceUrl();
      String username = hapiReferenceServerProperties.getDataSourceUsername();
      String password = hapiReferenceServerProperties.getDataSourcePassword();
      String schema = hapiReferenceServerProperties.getDataSourceSchema();

      Driver driver;
      driver = (Driver) Class.forName(driverName).getConstructor().newInstance();
      dataSource.setDriver(driver);
      dataSource.setUrl(url);
      dataSource.setUsername(username);
      dataSource.setPassword(password);
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

  @Override
  @Bean
  public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
    LocalContainerEntityManagerFactoryBean manager = super.entityManagerFactory();
    manager.setPersistenceUnitName("HAPI_PU");
    manager.setDataSource(dataSource());
    manager.setJpaProperties(jpaProperties());

    return manager;
  }

  private Properties jpaProperties() {

    HapiReferenceServerProperties hapiReferenceServerProperties = new HapiReferenceServerProperties();

    Properties properties = new Properties();
    properties.put("hibernate.dialect", hapiReferenceServerProperties.getHibernateDialect());
    properties.put("hibernate.format_sql", hapiReferenceServerProperties.getHibernateFormatSQL());
    properties.put("hibernate.show_sql", hapiReferenceServerProperties.getHibernateShowSQL());
    properties.put("hibernate.hbm2ddl.auto", hapiReferenceServerProperties.getHibernateHBM2DDLAuto());
    properties.put("hibernate.jdbc.batch_size", hapiReferenceServerProperties.getHibernateJDBCBatchSize());
    properties.put("hibernate.cache.use_query_cache", hapiReferenceServerProperties.getHibernateCacheUseQueryCache());
    properties.put("hibernate.cache.use_second_level_cache", hapiReferenceServerProperties.getHibernateCacheUseSecondLevelCache());
    properties.put("hibernate.cache.use_structured_entries", hapiReferenceServerProperties.getHibernateCacheUseStructuredEntries());
    properties.put("hibernate.cache.use_minimal_puts", hapiReferenceServerProperties.getHibernateCacheUseMinimalPuts());

    properties.put("hibernate.search.model_mapping", hapiReferenceServerProperties.getHibernateSearchModelMapping());
    properties.put("hibernate.search.default.directory_provider", hapiReferenceServerProperties.getHibernateSearchDefaultDirectoryProvider());
    properties.put("hibernate.search.default.indexBase", hapiReferenceServerProperties.getHibernateSearchDefaultIndexBase());
    properties.put("hibernate.search.lucene_version", hapiReferenceServerProperties.getHibernateSearchLuceneVersion());

    return properties;

  }

  @Bean
  public ResponseHighlighterInterceptor responseHighlighterInterceptor() {
    return new ResponseHighlighterInterceptor();
  }

  @Bean
  public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
    JpaTransactionManager manager = new JpaTransactionManager();
    manager.setEntityManagerFactory(entityManagerFactory);
    return manager;
  }
}
