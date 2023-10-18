package com.jit.defkoi.service;

public class NonUniqueObjectException extends Exception {

  public NonUniqueObjectException(String message) {
    super(message);
  }

  public NonUniqueObjectException() {
    super();
  }

}
