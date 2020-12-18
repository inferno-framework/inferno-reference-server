package org.mitre.fhir;

import ca.uhn.fhir.context.ConfigurationException;
import java.io.InputStream;
import java.util.Properties;

public class HapiReferenceServerProperties {

  private static final String DATA_SOURCE_DRIVER_NAME_KEY = "datasource.driver";
  private static final String DATA_SOURCE_URL_KEY = "datasource.url";
  private static final String DATA_SOURCE_USERNAME_KEY = "datasource.username";
  private static final String DATA_SOURCE_PASSWORD_KEY = "datasource.password";
  private static final String DATA_SOURCE_SCHEMA_KEY = "datasource.schema";
  private static final String DATA_SOURCE_MAX_TOTAL_KEY = "datasource.max_pool_size";

  private static final String HIBERNATE_DIALECT_KEY = "hibernate.dialect";
  private static final String HIBERNATE_FORMAT_SQL_KEY = "hibernate.format_sql";
  private static final String HIBERNATE_SHOW_SQL_KEY = "hibernate.show_sql";
  private static final String HIBERNATE_HBM2DDL_AUTO_KEY = "hibernate.hbm2ddl.auto";
  private static final String HIBERNATE_JDBC_BATCH_SIZE_KEY = "hibernate.jdbc.batch_size";
  private static final String HIBERNATE_CACHE_USE_QUERY_CACHE_KEY =
      "hibernate.cache.use_query_cache";
  private static final String HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE_KEY =
      "hibernate.cache.use_second_level_cache";
  private static final String HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES_KEY =
      "hibernate.cache.use_structured_entries";
  private static final String HIBERNATE_CACHE_USE_MINIMAL_PUTS_KEY =
      "hibernate.cache.use_minimal_puts";
  private static final String HIBERNATE_SEARCH_MODEL_MAPPING_KEY = "hibernate.search.model_mapping";
  private static final String HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER_KEY =
      "hibernate.search.default.directory_provider";
  private static final String HIBERNATE_SEARCH_DEFAULT_INDEX_BASE_KEY =
      "hibernate.search.default.indexBase";
  private static final String HIBERNATE_SEARCH_LUCENE_VERSION_KEY =
      "hibernate.search.lucene_version";
  private static final String HAPI_PROPERTIES = "hapi.properties";
  private static final String PUBLIC_CLIENT_ID_KEY = "inferno.public_client_id";
  private static final String CONFIDENTIAL_CLIENT_ID_KEY = "inferno.confidential_client_id";
  private static final String CONFIDENTIAL_CLIENT_SECRET_KEY = "inferno.confidential_client_secret";

  
  private final Properties properties;

  /**
   * Loads the server properties.
   */
  public HapiReferenceServerProperties() {
    try (InputStream in = HapiReferenceServerProperties
        .class
        .getClassLoader()
        .getResourceAsStream(HAPI_PROPERTIES)) {
      this.properties = new Properties();
      this.properties.load(in);
    } catch (Exception e) {
      throw new ConfigurationException("Could not load HAPI properties", e);
    }

  }

  /**
   * Returns the data source driver name.
   * @return the name of the data source driver
   */
  public String getDataSourceDriver() {
    String driverName = properties.getProperty(DATA_SOURCE_DRIVER_NAME_KEY);

    System.out.println("driverName is " + driverName);
    return driverName;
  }

  /**
   * Returns the URL of the data source.
   * @return the URL of the data source
   */
  public String getDataSourceUrl() {
    String url = properties.getProperty(DATA_SOURCE_URL_KEY);
    return url;
  }

  /**
   * Returns the username for the data source.
   * @return the username
   */
  public String getDataSourceUsername() {
    String username = properties.getProperty(DATA_SOURCE_USERNAME_KEY);
    return username;
  }

  /**
   * Returns the password for the data source.
   * @return the password
   */
  public String getDataSourcePassword() {
    String password = properties.getProperty(DATA_SOURCE_PASSWORD_KEY);
    return password;
  }

  /**
   * Returns the data source schema.
   * @return the schema
   */
  public String getDataSourceSchema() {
    String schema = properties.getProperty(DATA_SOURCE_SCHEMA_KEY);
    return schema;
  }

  /**
   * Returns the max pool size for the data source.
   * @return
   */
  public String getDataSourceMaxPoolSize() {
    String maxPoolSize = properties.getProperty(DATA_SOURCE_MAX_TOTAL_KEY);
    return maxPoolSize;
  }

  /**
   * Returns the Hibernate Dialect.
   * @return the dialect
   */
  public String getHibernateDialect() {
    String hibernateDialect = properties.getProperty(HIBERNATE_DIALECT_KEY);
    return hibernateDialect;
  }

  /**
   * Return the Hibernate SQL format.
   * @return the SQL format
   */
  public String getHibernateFormatSql() {
    String hibernateFormatSql = properties.getProperty(HIBERNATE_FORMAT_SQL_KEY);
    return hibernateFormatSql;
  }

  /**
   * Returns the Hibernate show SQL key.
   * @return the show SQL key
   */
  public String getHibernateShowSql() {
    String hibernateShowSql = properties.getProperty(HIBERNATE_SHOW_SQL_KEY);
    return hibernateShowSql;
  }

  /**
   * Returns the Hibernate HBM2DDLAuto key.
   * @return the HBM2DDLAuto key
   */
  public String getHibernateHbdm2ddlAuto() {
    String hibernateHbdm2ddlAuto = properties.getProperty(HIBERNATE_HBM2DDL_AUTO_KEY);
    return hibernateHbdm2ddlAuto;
  }

  /**
   * Returns the Hibernate JDBC Batch Size.
   * @return the batch size
   */
  public String getHibernateJdbcBatchSize() {
    String hibernateJdbcBatchSize = properties.getProperty(HIBERNATE_JDBC_BATCH_SIZE_KEY);
    return hibernateJdbcBatchSize;
  }

  /**
   * Returns the Hibernate cache use query cache.
   * @return the cache use query cache
   */
  public String getHibernateCacheUseQueryCache() {
    String hibernateCacheUseQueryCache = properties
        .getProperty(HIBERNATE_CACHE_USE_QUERY_CACHE_KEY);
    return hibernateCacheUseQueryCache;
  }

  /**
   * Returns the Hibernate cache use second level cache.
   * @return the property
   */
  public String getHibernateCacheUseSecondLevelCache() {
    String hibernateCacheUseSecondLevelCache = properties
        .getProperty(HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE_KEY);
    return hibernateCacheUseSecondLevelCache;
  }

  /**
   * Returns the Hibernate Property.
   * @return the property
   */
  public String getHibernateCacheUseStructuredEntries() {
    String hibernateCacheUseStructuredEntries = properties
        .getProperty(HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES_KEY);
    return hibernateCacheUseStructuredEntries;
  }

  /**
   * Returns the Hibernate Property.
   * @return the property
   */
  public String getHibernateCacheUseMinimalPuts() {
    String hibernateCacheUseMinimalPuts = properties
        .getProperty(HIBERNATE_CACHE_USE_MINIMAL_PUTS_KEY);
    return hibernateCacheUseMinimalPuts;
  }

  /**
   * Returns the Hibernate Property.
   * @return the property
   */
  public String getHibernateSearchModelMapping() {
    String hibernateSearchModelMapping = properties
        .getProperty(HIBERNATE_SEARCH_MODEL_MAPPING_KEY);
    return hibernateSearchModelMapping;
  }

  /**
   * Returns the Hibernate Property.
   * @return the property
   */
  public String getHibernateSearchDefaultDirectoryProvider() {
    String hibernateSearchDefaultDirectoryProvider = properties
        .getProperty(HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER_KEY);
    return hibernateSearchDefaultDirectoryProvider;
  }

  /**
   * Returns the Hibernate Property.
   * @return the property
   */
  public String getHibernateSearchDefaultIndexBase() {
    String hibernateSearchDefaultIndexBase = properties
        .getProperty(HIBERNATE_SEARCH_DEFAULT_INDEX_BASE_KEY);
    return hibernateSearchDefaultIndexBase;
  }

  /**
   * Returns the Hibernate Property.
   * @return the property
   */
  public String getHibernateSearchLuceneVersion() {
    String hibernateSearchLuceneVersion = properties
        .getProperty(HIBERNATE_SEARCH_LUCENE_VERSION_KEY);
    return hibernateSearchLuceneVersion;
  }
  
  /**
   * Returns the public Client ID Property.
   * @return the property
   */ 
  public String getPublicClientId() {
    String publicClientId = properties
        .getProperty(PUBLIC_CLIENT_ID_KEY);
    return publicClientId;
  }

  /**
   * Returns the Hibernate Property.
   * @return the property
   */ 
  public String getConfidentialClientId() {
    String confidentialClientId = properties
        .getProperty(CONFIDENTIAL_CLIENT_ID_KEY);
    return confidentialClientId;
  }

  /**
   * Returns the Hibernate Property.
   * @return the property
   */ 
  public String getConfidentialClientSecret() {
    String confidentialClientSecret = properties
        .getProperty(CONFIDENTIAL_CLIENT_SECRET_KEY);
    return confidentialClientSecret;
  }
  
  
  /**
   * Returns the properties.
   * @return the properties
   */
  public Properties getProperties() {
    return properties;
  }
}
