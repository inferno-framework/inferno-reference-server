package org.mitre.fhir;

import ca.uhn.fhir.jpa.config.BaseJavaConfigR4;
import ca.uhn.fhir.jpa.dao.DaoConfig;
import ca.uhn.fhir.jpa.model.entity.ModelConfig;
import ca.uhn.fhir.jpa.search.LuceneSearchMappingFactory;
import ca.uhn.fhir.jpa.util.SubscriptionsRequireManualActivationInterceptorR4;
import ca.uhn.fhir.rest.server.interceptor.IServerInterceptor;
import ca.uhn.fhir.rest.server.interceptor.ResponseHighlighterInterceptor;
import org.apache.commons.dbcp2.BasicDataSource;
import org.hibernate.dialect.PostgreSQL9Dialect;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Map;
import java.util.Properties;

/**
 * @author Tim Shaffer
 */
@Configuration
@EnableTransactionManagement
public class MitreServerConfig extends BaseJavaConfigR4 {

    // Uncomment this to only support certain resource types.
    // @Override
    // protected boolean isSupported(String resourceType) {
    // 	return resourceType.startsWith("Patient") || resourceType.startsWith("Account");
    // }

    @Bean
    public DaoConfig daoConfig() {
        DaoConfig config = new DaoConfig();
        // config.setAllowMultipleDelete(true);
        // config.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.RESTHOOK);
        // config.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.WEBSOCKET);
        // config.addSupportedSubscriptionType(Subscription.SubscriptionChannelType.EMAIL);
        // config.setSubscriptionMatchingEnabled(true);

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
        // Get database connection from environment variables.
        Map<String, String> environment = System.getenv();
        String host = environment.getOrDefault("POSTGRES_HOST", "localhost");
        String port = environment.getOrDefault("POSTGRES_PORT", "5432");
        String user = environment.getOrDefault("POSTGRES_USER", "postgres");
        String password = environment.getOrDefault("POSTGRES_PASSWORD", "welcome123");
        String database = environment.getOrDefault("POSTGRES_DB", "postgres");
        String schema = environment.getOrDefault("POSTGRES_SCHEMA", "public");

        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriver(new org.postgresql.Driver());
        dataSource.setUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        dataSource.setDefaultSchema(schema);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
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
        Properties properties = new Properties();
        properties.put("hibernate.dialect", PostgreSQL9Dialect.class.getName());
        properties.put("hibernate.format_sql", "true");
        properties.put("hibernate.show_sql", "false");
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.jdbc.batch_size", "20");
        properties.put("hibernate.cache.use_query_cache", "false");
        properties.put("hibernate.cache.use_second_level_cache", "false");
        properties.put("hibernate.cache.use_structured_entries", "false");
        properties.put("hibernate.cache.use_minimal_puts", "false");
        // TODO: Configure lucerne search directory.
        properties.put("hibernate.search.model_mapping", LuceneSearchMappingFactory.class.getName());
        properties.put("hibernate.search.default.directory_provider", "filesystem");
        properties.put("hibernate.search.default.indexBase", "target/lucenefiles");
        properties.put("hibernate.search.lucene_version", "LUCENE_CURRENT");
        // extraProperties.put("hibernate.search.default.worker.execution", "async");
        return properties;
    }

    @Bean
    public ResponseHighlighterInterceptor responseHighlighterInterceptor() {
        return new ResponseHighlighterInterceptor();
    }

    // @Bean
    // public IServerInterceptor subscriptionSecurityInterceptor() {
    //     return new SubscriptionsRequireManualActivationInterceptorR4();
    // }

    @Bean
    public JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager manager = new JpaTransactionManager();
        manager.setEntityManagerFactory(entityManagerFactory);
        return manager;
    }
}
