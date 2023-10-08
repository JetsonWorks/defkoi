package com.jit.defkoi.rest;

import lombok.Getter;
import org.springframework.context.MessageSource;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;

@Getter
public class RestException extends Exception {

  public static final String generalErrorKey = "generalError";

  private Map<String, String> errorMap = new Hashtable<>();

  public RestException(String key, String message) {
    errorMap.put(key, message);
  }

  public RestException(String message, Map<String, String> errorMap) {
    super(message);
    this.errorMap = errorMap;
  }

  public static RestException build(BindingResult bindingResult, MessageSource messageSource, Locale locale) {
    StringBuilder sb = new StringBuilder();
    Map<String, String> errorMap = new Hashtable<>();
    for(FieldError err : bindingResult.getFieldErrors()) {
      sb.append(err.toString());
      errorMap.put(err.getField(), messageSource.getMessage(err, locale));
    }
    return new RestException(sb.toString(), errorMap);
  }

  public RestException(String message) {
    super(message);
  }

  public RestException(String message, Throwable cause) {
    super(message, cause);
  }

  @Override
  public String toString() {
    return getMessage();
  }

}
