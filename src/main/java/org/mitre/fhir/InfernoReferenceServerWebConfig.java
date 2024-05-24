package org.mitre.fhir;

import ca.uhn.fhir.to.mvc.AnnotationMethodHandlerAdapterConfigurer;
import ca.uhn.fhir.to.util.WebUtil;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.spring6.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring6.view.ThymeleafViewResolver;
import org.thymeleaf.templatemode.TemplateMode;

// Based off of ca.uhn.fhir.to.FhirTesterMvcConfig
@Configuration
@EnableWebMvc
public class InfernoReferenceServerWebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry theRegistry) {
    WebUtil.webJarAddBoostrap(theRegistry);
    WebUtil.webJarAddJQuery(theRegistry);
    WebUtil.webJarAddFontAwesome(theRegistry);
    WebUtil.webJarAddJSTZ(theRegistry);
    WebUtil.webJarAddEonasdanBootstrapDatetimepicker(theRegistry);
    WebUtil.webJarAddMomentJS(theRegistry);
    WebUtil.webJarAddSelect2(theRegistry);
    WebUtil.webJarAddAwesomeCheckbox(theRegistry);

    theRegistry.addResourceHandler("/css/**").addResourceLocations("/css/");
    theRegistry.addResourceHandler("/fa/**").addResourceLocations("/fa/");
    theRegistry.addResourceHandler("/fonts/**").addResourceLocations("/fonts/");
    theRegistry.addResourceHandler("/img/**").addResourceLocations("/img/");
    theRegistry.addResourceHandler("/js/**").addResourceLocations("/js/");
  }

  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    // This method restores the previous default setting,
    // where a trailing slash may be ignored in path matching.
    // eg: /page and /page/ will both resolve to the same mapping.
    // Note that this setting is deprecated, and if it is eventually removed
    // we will have to manually configure all relevant endpoints
    // to match with or without a trailing slash.
    configurer.setUseTrailingSlashMatch(true);
  }

  /**
   * Configuration for the templateResolver.
   * 
   * @return SpringResourceTemplateResolver
   */
  @Bean
  public SpringResourceTemplateResolver templateResolver() {
    SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
    resolver.setPrefix("/WEB-INF/templates/");
    resolver.setSuffix(".html");
    resolver.setTemplateMode(TemplateMode.HTML);
    resolver.setCharacterEncoding("UTF-8");
    return resolver;
  }

  /**
   * Configuration for AnnotationMethodHandlerAdapterConfigurer.
   * 
   * @return AnnotationMethodHandlerAdapterConfigurer
   */
  @Bean
  public AnnotationMethodHandlerAdapterConfigurer annotationMethodHandlerAdapterConfigurer(
      @Qualifier("requestMappingHandlerAdapter") RequestMappingHandlerAdapter theAdapter) {
    return new AnnotationMethodHandlerAdapterConfigurer(theAdapter);
  }

  /**
   * Configuration of ThymeleafViewResolver.
   * 
   * @return ThymeleafViewResolver
   */
  @Bean
  public ThymeleafViewResolver viewResolver() {
    ThymeleafViewResolver viewResolver = new ThymeleafViewResolver();
    viewResolver.setTemplateEngine(templateEngine());
    viewResolver.setCharacterEncoding("UTF-8");
    return viewResolver;
  }

  /**
   * Configuration of SpringTemplateEngine.
   * 
   * @return SpringTemplateEngine.
   */
  @Bean
  public SpringTemplateEngine templateEngine() {
    SpringTemplateEngine templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver());

    return templateEngine;
  }
}
