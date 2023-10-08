package com.jit.defkoi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Configuration
@Data
@NoArgsConstructor
public class Detect {

  @Value("#{true}")
  private Boolean objDetectEnabled;
  @Value("#{960}")
  private Integer maxObjDetectWidth;
  @Value("#{540}")
  private Integer maxObjDetectHeight;
  @Value("#{3}")
  private Integer objectSquelchPeriod;

  @Value("#{false}")
  private Boolean motDetectEnabled;
  @Value("#{true}")
  private Boolean motionGrayscale;
  @Value("#{480}")
  private Integer maxMotDetectWidth;
  @Value("#{270}")
  private Integer maxMotDetectHeight;

  /** e.g. Pytorch */
  private String engine;
  /** e.g. resnet */
  private String artifactId;
  /** e.g. resnet18 */
  private String backbone;
  /** e.g. v1b */
  private String flavor;
  /** e.g. coco */
  private String dataset;
  @Value("#{0.5}")
  private String argThreshold;
  @Value("#{false}")
  private Boolean saveBoundingBoxImage;

  public Detect scrub() throws InvocationTargetException, IllegalAccessException {
    Detect c = new Detect();
    BeanUtils.copyProperties(c, this);
    return c;
  }

  public void patch(Detect config) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
    Map<String, String> props = BeanUtils.describe(config);
    for(String key : props.keySet()) {
      if(props.get(key) != null)
        BeanUtils.setProperty(this, key, props.get(key));
    }
  }

}
