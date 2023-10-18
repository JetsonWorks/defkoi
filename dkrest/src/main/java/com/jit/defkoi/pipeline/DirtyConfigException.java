package com.jit.defkoi.pipeline;

import com.jit.defkoi.DefKoiException;

public class DirtyConfigException extends DefKoiException {

  public DirtyConfigException(String message) {
    super(message);
  }

  public DirtyConfigException(String message, Throwable cause) {
    super(message, cause);
  }

}
