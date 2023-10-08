package com.jit.defkoi.pipeline;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
public class PipelineStats {
  private static int defaultExpiry = 5000;

  private String observed;

  private Metric motAccept;
  private Metric motConversion;
  private Metric motProcess;
  private Metric motFramesDetected;

  private Metric objAccept;
  private Metric objConversion;
  private Metric objProcess;
  private Metric objFramesDetected;

  private Metric objPubAccept;
  private Metric objPubProcess;

  static {
    Metric.setDefaultExpiry(defaultExpiry);
  }

  public PipelineStats(String observed) {
    this.observed = observed;

    motAccept = new Metric();
    motConversion = new Metric();
    motProcess = new Metric();
    motFramesDetected = new Metric();

    objAccept = new Metric();
    objConversion = new Metric();
    objProcess = new Metric();
    objFramesDetected = new Metric();

    objPubAccept = new Metric();
    objPubProcess = new Metric();
  }

  public double getDefaultExpirySeconds() {
    return BigDecimal.valueOf((double)defaultExpiry / 1000).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

}
