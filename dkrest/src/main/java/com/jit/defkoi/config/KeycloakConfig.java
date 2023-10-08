package com.jit.defkoi.config;

import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.springboot.KeycloakSpringBootConfigResolver;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.web.session.HttpSessionEventPublisher;

@KeycloakConfiguration
@EnableAutoConfiguration
public class KeycloakConfig {

  /* httpSessionEventPublisher, configureGlobal, sessionAuthenticationStrategy, and keycloakConfigResolver
     used by Keycloak, per https://www.keycloak.org/docs/latest/securing_apps/index.html#_spring_security_adapter
   */

  @Bean
  public ServletListenerRegistrationBean<HttpSessionEventPublisher> httpSessionEventPublisher() {
    return new ServletListenerRegistrationBean<HttpSessionEventPublisher>(new HttpSessionEventPublisher());
  }

  @Bean
  public KeycloakConfigResolver keycloakConfigResolver() {
    return new KeycloakSpringBootConfigResolver();
  }

  /* Client to Client Support
    To simplify communication between clients, Keycloak provides an extension of Spring’s RestTemplate that handles
    bearer token authentication for you. To enable this feature your security configuration must add the
    KeycloakRestTemplate bean. Note that it must be scoped as a prototype to function correctly.
    Your application code can then use KeycloakRestTemplate any time it needs to make a call to another client.
  @Autowired
  public KeycloakClientRequestFactory keycloakClientRequestFactory;

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  public KeycloakRestTemplate keycloakRestTemplate() {
    return new KeycloakRestTemplate(keycloakClientRequestFactory);
  }
  */

}

