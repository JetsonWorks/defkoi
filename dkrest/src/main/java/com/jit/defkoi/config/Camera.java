package com.jit.defkoi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

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

  public Camera scrub() throws InvocationTargetException, IllegalAccessException {
    Camera c = new Camera();
    BeanUtils.copyProperties(c, this);
    return c;
  }

  public void patch(Camera config) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Map<String, String> props = BeanUtils.describe(config);
    for(String key : props.keySet()) {
      if(props.get(key) != null)
        BeanUtils.setProperty(this, key, props.get(key));
    }
  }

}
