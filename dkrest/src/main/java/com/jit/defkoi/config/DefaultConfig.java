package com.jit.defkoi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Configuration
@Data
@ConfigurationProperties(prefix = "defkoi")
@NoArgsConstructor
public class DefaultConfig {

  /* Setting debug will enable a V4L2 device cap dump before the pipeline is constructed and a DOT file dump of the
  pipeline after construction. */
  @Value("#{false}")
  private Boolean debug;
  @Value("#{'media'}")
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
  @Value("#{'rtsp://rtspProxy:8554/'}")
  private String rtspProxyUrl;

  public DefaultConfig scrub() throws InvocationTargetException, IllegalAccessException {
    DefaultConfig c = new DefaultConfig();
    BeanUtils.copyProperties(c, this);
    return c;
  }

  public void patch(DefaultConfig defaultConfig)
    throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Map<String, String> props = BeanUtils.describe(defaultConfig);
    for(String key : props.keySet()) {
      if(props.get(key) != null)
        BeanUtils.setProperty(this, key, props.get(key));
    }
  }

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