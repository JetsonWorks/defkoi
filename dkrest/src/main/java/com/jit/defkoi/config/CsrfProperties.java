package com.jit.defkoi.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "csrf")
@Getter
@Setter
public class CsrfProperties {

  private String cookieName = "XSRF-TOKEN";
  private String cookiePath = "/";
  private String headerName = "X-XSRF-TOKEN";

}

