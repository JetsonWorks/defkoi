package com.jit.defkoi.pipeline;

import lombok.Getter;

public enum ImageFormat {
  gray("GRAY8"), color("BGRx"), i420("I420");

  @Getter
  private String gstFormatString;

  ImageFormat(String gstFormatString) {
    this.gstFormatString = gstFormatString;
  }

}
