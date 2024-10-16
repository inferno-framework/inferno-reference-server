package org.mitre.fhir;

import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import ca.uhn.fhir.rest.server.interceptor.InterceptorAdapter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsProcessor;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.DefaultCorsProcessor;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Adapted from ca.uhn.fhir.rest.server.interceptor.CorsInterceptor
 * Modified to allow different configuration based on URL
 */
public class PathBasedCorsInterceptor extends InterceptorAdapter {

  private final CorsProcessor corsProcessor;
  private LinkedHashMap<String, CorsConfiguration> config;

  
  public PathBasedCorsInterceptor(LinkedHashMap<String, CorsConfiguration> config) {
    corsProcessor = new DefaultCorsProcessor();
    this.config = config;
  }
  
  static boolean pathMatches(PathContainer requestUri, String configuredPath) {
    PathPattern parsedConfiguredPath = PathPatternParser.defaultInstance.parse(configuredPath);
    return parsedConfiguredPath.matches(requestUri);
  }
  
  static boolean pathMatches(String requestUri, String configuredPath) {
    return pathMatches(PathContainer.parsePath(requestUri), configuredPath);
  }
  
  @Override
  public boolean incomingRequestPreProcessed(HttpServletRequest req, HttpServletResponse resp) {
    if (CorsUtils.isCorsRequest(req)) {
      boolean isValid = true;
      try {
        PathContainer uri = PathContainer.parsePath(req.getRequestURI());
        for (String path : config.keySet()) {
          if (pathMatches(uri, path)) {
            isValid = corsProcessor.processRequest(config.get(path), req, resp);
            break;
          }
        }
      } catch (IOException e) {
        // TODO: investigate what might cause an exception. For now just 500 error
        throw new InternalErrorException("Exception occurred in CORS handling", e);
      }
      if (!isValid || CorsUtils.isPreFlightRequest(req)) {
        return false;
      }
    }

    return super.incomingRequestPreProcessed(req, resp);
  }

  /**
   * Helper method for a CorsConfiguration with common options pre-configured.
   * Note this does not set any Allowed-Origin options, callers must set that.
   */
  public static CorsConfiguration baseConfig() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedHeader(HttpHeaders.ORIGIN);
    config.addAllowedHeader(HttpHeaders.ACCEPT);
    config.addAllowedHeader(HttpHeaders.CONTENT_TYPE);
    config.addAllowedHeader(HttpHeaders.AUTHORIZATION);
    config.addAllowedHeader(HttpHeaders.CACHE_CONTROL);
    config.addAllowedHeader("x-fhir-starter");
    config.addAllowedHeader("X-Requested-With");
    config.addAllowedHeader("Prefer");
    config.addExposedHeader("Location");
    config.addExposedHeader("Content-Location");
    config.setAllowedMethods(
        Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

    return config;
  }
  
  /**
   * Helper function to create a CorsConfiguration for public "discovery" type endpoints.
   * This is intended to allow access from any origin.
   */
  public static CorsConfiguration publicDiscoveryEndpointConfig() {
    CorsConfiguration publicDiscoveryEndpointConfig = baseConfig();
    
    // using "Allowed Origin" here results in the literal response
    // Access-Control-Allow-Origin: *
    publicDiscoveryEndpointConfig.addAllowedOrigin("*");
    
    // this is the default, but making it explicit here
    publicDiscoveryEndpointConfig.setAllowCredentials(false);
    return publicDiscoveryEndpointConfig;
  }
  
  /**
   * Helper function to create a CorsConfiguration for private or API endpoints,
   * where access should only be allowed from registered client origins.
   */
  public static CorsConfiguration privateApiEndpointConfig() {
    CorsConfiguration privateApiEndpointConfig = baseConfig();
    
    // using "Allowed Origin Pattern" here results in
    // the Origin field from the request being echoed back in the response, ex:
    // Access-Control-Allow-Origin: http://localhost:4567
    privateApiEndpointConfig.addAllowedOriginPattern("*");
    
    privateApiEndpointConfig.setAllowCredentials(true);
    return privateApiEndpointConfig;
  }
  
}
