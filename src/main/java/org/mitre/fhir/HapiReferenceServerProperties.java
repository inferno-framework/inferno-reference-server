package org.mitre.fhir;

import java.io.InputStream;
import java.util.Properties;

import ca.uhn.fhir.context.ConfigurationException;

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
	private static final String HIBERNATE_CACHE_USE_QUERY_CACHE_KEY = "hibernate.cache.use_query_cache";
	private static final String HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE_KEY = "hibernate.cache.use_second_level_cache";
	private static final String HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES_KEY = "hibernate.cache.use_structured_entries";
	private static final String HIBERNATE_CACHE_USE_MINIMAL_PUTS_KEY = "hibernate.cache.use_minimal_puts";
	private static final String HIBERNATE_SEARCH_MODEL_MAPPING_KEY = "hibernate.search.model_mapping";
	private static final String HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER_KEY = "hibernate.search.default.directory_provider";
	private static final String HIBERNATE_SEARCH_DEFAULT_INDEX_BASE_KEY = "hibernate.search.default.indexBase";
	private static final String HIBERNATE_SEARCH_LUCENE_VERSION_KEY = "hibernate.search.lucene_version";
	
	
    private Properties properties;
    
    
    private static final String HAPI_PROPERTIES = "hapi.properties";

    public HapiReferenceServerProperties()
    {
        try (InputStream in = HapiReferenceServerProperties.class.getClassLoader().getResourceAsStream(HAPI_PROPERTIES)){
            this.properties = new Properties();
            this.properties.load(in);
        } catch (Exception e) {
            throw new ConfigurationException("Could not load HAPI properties", e);
        }
    	
    }
    
    public String getDataSourceDriver()
    {
    	String driverName = properties.getProperty(DATA_SOURCE_DRIVER_NAME_KEY);
    	
    	System.out.println("driverName is " + driverName);
    	return driverName;
    }
    
    public String getDataSourceUrl()
    {
    	String url = properties.getProperty(DATA_SOURCE_URL_KEY);
    	return url;
    }
    
    public String getDataSourceUsername()
    {
    	String username = properties.getProperty(DATA_SOURCE_USERNAME_KEY);
    	return username;
    }
    
    public String getDataSourcePassword()
    {
    	String password = properties.getProperty(DATA_SOURCE_PASSWORD_KEY);
    	return password;
    }
    
    public String getDataSourceSchema()
    {
    	String schema = properties.getProperty(DATA_SOURCE_SCHEMA_KEY);
    	return schema;
    }
    
    public String getDataSourceMaxPoolSize()
    {
    	String maxPoolSize = properties.getProperty(DATA_SOURCE_MAX_TOTAL_KEY);
    	return maxPoolSize;
    }
    
    public String getHibernateDialect()
    {
    	String hibernateDialect = properties.getProperty(HIBERNATE_DIALECT_KEY);
    	return hibernateDialect;
    }
    
    public String getHibernateFormatSQL()
    {
    	String hibernateFormatSQL = properties.getProperty(HIBERNATE_FORMAT_SQL_KEY);
    	return hibernateFormatSQL;
    }
    
    public String getHibernateShowSQL()
    {
    	String hibernateShowSQL = properties.getProperty(HIBERNATE_SHOW_SQL_KEY);
    	return hibernateShowSQL;
    }
    
    public String getHibernateHBM2DDLAuto()
    {
    	String hibernateHBDM2DDLAuto = properties.getProperty(HIBERNATE_HBM2DDL_AUTO_KEY);
    	return hibernateHBDM2DDLAuto;
    }
    
    public String getHibernateJDBCBatchSize()
    {
    	String hibernateJDBCBatchSize = properties.getProperty(HIBERNATE_JDBC_BATCH_SIZE_KEY);
    	return hibernateJDBCBatchSize;
    }
    
    public String getHibernateCacheUseQueryCache()
    {
    	String hibernateCacheUseQueryCache = properties.getProperty(HIBERNATE_CACHE_USE_QUERY_CACHE_KEY);
    	return hibernateCacheUseQueryCache;
    }
    
    public String getHibernateCacheUseSecondLevelCache()
    {
    	String hibernateCacheUseSecondLevelCache = properties.getProperty(HIBERNATE_CACHE_USE_SECOND_LEVEL_CACHE_KEY);
    	return hibernateCacheUseSecondLevelCache;
    }
    
    public String getHibernateCacheUseStructuredEntries()
    {
    	String hibernateCacheUseStructuredEntries = properties.getProperty(HIBERNATE_CACHE_USE_STRUCTURED_ENTRIES_KEY);
    	return hibernateCacheUseStructuredEntries;
    }
    
    public String getHibernateCacheUseMinimalPuts()
    {
    	String hibernateCacheUseMinimalPuts = properties.getProperty(HIBERNATE_CACHE_USE_MINIMAL_PUTS_KEY);
    	return hibernateCacheUseMinimalPuts;
    }
    
    public String getHibernateSearchModelMapping()
    {
    	String hibernateSearchModelMapping = properties.getProperty(HIBERNATE_SEARCH_MODEL_MAPPING_KEY);
    	return hibernateSearchModelMapping;
    }
    
    public String getHibernateSearchDefaultDirectoryProvider()
    {
    	String hibernateSearchDefaultDirectoryProvider = properties.getProperty(HIBERNATE_SEARCH_DEFAULT_DIRECTORY_PROVIDER_KEY);
    	return hibernateSearchDefaultDirectoryProvider;
    }
    
    public String getHibernateSearchDefaultIndexBase()
    {
    	String hibernateSearchDefaultIndexBase = properties.getProperty(HIBERNATE_SEARCH_DEFAULT_INDEX_BASE_KEY);
    	return hibernateSearchDefaultIndexBase;
    }
    
    public String getHibernateSearchLuceneVersion()
    {
    	String hibernateSearchLuceneVersion = properties.getProperty(HIBERNATE_SEARCH_LUCENE_VERSION_KEY);
    	return hibernateSearchLuceneVersion;
    }
    
    public Properties getProperties()
    {
    	return properties;
    }
    
    

}
