package com.jit.defkoi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Data
@ConfigurationProperties(prefix = "defkoi")
@NoArgsConstructor
public class DefaultConfig {

  /* Setting debug will enable a V4L2 device cap dump before the pipeline is constructed and a DOT file dump of the
  pipeline after construction. */
  @Value("#{false}")
  private Boolean debug;
  @Value("#{'/var/media'}")
  private String mediaDir;
  @Value("#{100}")
  private Integer maxImages;
  @Value("#{200}")
  private Integer maxVideos;
  @Value("#{60}")
  private Integer videoLength;
  @Value("#{'labels'}")
  private String labelFile;
  @Value("#{false}")
  private Boolean logStatsEnabled;
  @Value("#{15}")
  private Integer statsUpdatePeriod;
  @Value("#{1}")
  private Integer queueMaxSize;
  @Value("#{'rtsp://defkon.jit.com:8554/'}")
  private String rtspProxyUrl;

  @Bean
  @ConfigurationProperties("defkoi.camera")
  public Camera getCameraConfig() {
    return new Camera();
  }

  @Bean
  @ConfigurationProperties("defkoi.detection")
  public Detect getDetectConfig() {
    return new Detect();
  }

}
