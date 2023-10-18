package com.jit.defkoi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/** Default camera config. */
@Configuration
@Data
@NoArgsConstructor
public class Camera {

  @Value("#{1920}")
  private Integer maxCaptureWidth;
  @Value("#{1080}")
  private Integer maxCaptureHeight;
  @Value("#{false}")
  private Boolean tapLiveEnabled;
  @Value("#{false}")
  private Boolean liveRtspEnabled;
  @Value("#{false}")
  private Boolean objectRtspEnabled;

}
