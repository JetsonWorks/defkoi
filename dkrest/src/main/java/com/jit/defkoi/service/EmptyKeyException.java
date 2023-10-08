package com.jit.defkoi.service;

public class EmptyKeyException extends Exception {

  public EmptyKeyException(String message) {
    super(message);
  }

  public EmptyKeyException(String message, Throwable cause) {
    super(message, cause);
  }

  public EmptyKeyException() {
    super();
  }

}
