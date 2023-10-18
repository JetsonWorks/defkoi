package com.jit.defkoi.pipeline;

import lombok.Data;

import java.util.Date;

@Data
public class Datum {
  private Date timeStamp;
  private Double value;

  public Datum(double value) {
    this.timeStamp = new Date();
    this.value = value;
  }

}
