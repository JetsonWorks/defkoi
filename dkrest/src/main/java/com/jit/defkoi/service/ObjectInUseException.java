package com.jit.defkoi.service;

public class ObjectInUseException extends Exception {

  public ObjectInUseException() {
    super();
  }

  public ObjectInUseException(String message) {
    super(message);
  }

  public ObjectInUseException(Throwable cause) {
    super(cause);
  }

  public ObjectInUseException(String message, Throwable cause) {
    super(message, cause);
  }

  public ObjectInUseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
