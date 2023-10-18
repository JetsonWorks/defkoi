package com.jit.defkoi.config;

import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.keycloak.adapters.springsecurity.config.KeycloakWebSecurityConfigurerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.util.StringUtils;

@KeycloakConfiguration
@EnableAutoConfiguration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class DefKoiRestSecurity extends KeycloakWebSecurityConfigurerAdapter {

  @Autowired
  protected CsrfProperties csrfProperties;

  @Override
  protected void configure(final HttpSecurity http) throws Exception {
    super.configure(http);
    http.antMatcher("/**").authorizeRequests().antMatchers("/error**").permitAll();
    configureCsrf(http);
    configureCustomAuth(http);

    http.headers().frameOptions().sameOrigin();
    http.cors();
  }

  protected void configureCustomAuth(final HttpSecurity http) throws Exception {
    // @formatter:off
    http.antMatcher("/**").authorizeRequests()
      .antMatchers("/").hasRole("DEFKOI_USER")
      .anyRequest().authenticated()
    ;
    // @formatter:on
  }

  protected void configureCsrf(HttpSecurity http) throws Exception {
    CookieCsrfTokenRepository repo = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repo.setCookieHttpOnly(false);
    if(StringUtils.hasLength(csrfProperties.getCookieName()))
      repo.setCookieName(csrfProperties.getCookieName());
    if(StringUtils.hasLength(csrfProperties.getCookieDomain()))
      repo.setCookieDomain(csrfProperties.getCookieDomain());
    if(StringUtils.hasLength(csrfProperties.getCookiePath()))
      repo.setCookiePath(csrfProperties.getCookiePath());
    if(StringUtils.hasLength(csrfProperties.getHeaderName()))
      repo.setHeaderName(csrfProperties.getHeaderName());
    http.csrf().csrfTokenRepository(repo);
  }

  /* httpSessionEventPublisher, configureGlobal, sessionAuthenticationStrategy, and keycloakConfigResolver
     used by Keycloak, per https://www.keycloak.org/docs/latest/securing_apps/index.html#_spring_security_adapter
   */

  @Autowired
  public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
    auth.authenticationProvider(keycloakAuthenticationProvider());
  }

  @Bean
  @Override
  protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
    return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
  }

}

