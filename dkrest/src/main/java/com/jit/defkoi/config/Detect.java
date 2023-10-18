package com.jit.defkoi.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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

}
