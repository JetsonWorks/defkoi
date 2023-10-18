package com.jit.defkoi.config;

import com.jit.defkoi.jpa.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.rest.core.config.RepositoryRestConfiguration;
import org.springframework.data.rest.webmvc.config.RepositoryRestConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;

@Configuration
public class ExposeEntityIdRestConfiguration {

  @Bean
  public RepositoryRestConfigurer repositoryRestConfigurer() {

    return new RepositoryRestConfigurer() {

      @Override
      public void configureRepositoryRestConfiguration(RepositoryRestConfiguration config, CorsRegistry cors) {
        config.exposeIdsFor(UserEvent.class);
        config.exposeIdsFor(User.class);
        config.exposeIdsFor(Device.class);
        config.exposeIdsFor(Capability.class);
        config.exposeIdsFor(PipeConf.class);
      }
    };
  }
}
