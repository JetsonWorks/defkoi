package com.jit.defkoi;

public class DefKoiException extends Exception {

  public DefKoiException(String message) {
    super(message);
  }

  public DefKoiException(String message, Throwable cause) {
    super(message, cause);
  }

  public static DefKoiException deviceNoCaps(String name) {
    return new DefKoiException("Device " + name + " has no capabilities");
  }

}
