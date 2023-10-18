package com.jit.defkoi.rest;

import com.jit.defkoi.DefKoiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.nio.file.AccessDeniedException;
import java.util.Hashtable;
import java.util.Map;

import static com.jit.defkoi.rest.RestException.generalErrorKey;

@ControllerAdvice
public class ExceptionHandler {

  private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);

  @Autowired
  private MessageSource messageSource;

  public ExceptionHandler() {
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(DefKoiException.class)
  public ResponseEntity<Map<String, String>> handleDefKOiException(final DefKoiException e) {
    logger.error(e.toString(), e);
    return new ResponseEntity(e.toString(), HttpStatus.BAD_REQUEST);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(RestException.class)
  public ResponseEntity<Map<String, String>> handleRestException(final RestException e) {
    logger.debug(e.toString(), e);
    return new ResponseEntity(e.getErrorMap(), HttpStatus.BAD_REQUEST);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> handleAccessDeniedException(final AccessDeniedException e) {
    logger.error(e.toString(), e);
    return new ResponseEntity(e.toString(), HttpStatus.FORBIDDEN);
  }

  @org.springframework.web.bind.annotation.ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> handleException(final Exception e) {
    logger.error(e.toString(), e);
    return new ResponseEntity(exceptionErrorMap(generalErrorKey, e.toString()), HttpStatus.BAD_REQUEST);
  }

  public static Map<String, String> exceptionErrorMap(String key, String message) {
    Map<String, String> errors = new Hashtable<>();
    errors.put(key, message);
    return errors;
  }

}
