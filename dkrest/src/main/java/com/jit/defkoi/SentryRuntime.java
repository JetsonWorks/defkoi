package com.jit.defkoi;

import lombok.Data;
import org.slf4j.Logger;

@Data
public class SentryRuntime {

  private Logger logger;
  private Directive directive = Directive.stop;
  private Status status = Status.stopped;
  private String name;

  public SentryRuntime(Logger logger) {
    this.logger = logger;
  }

  public SentryRuntime(Logger logger, String name) {
    this.logger = logger;
    this.name = name;
  }

  public void start() {
    directive = Directive.start;
    logDirective();
  }

  public void run() {
    directive = Directive.run;
    logDirective();
  }

  public void reinitialize() {
    directive = Directive.reinitialize;
    logDirective();
  }

  public void stop() {
    directive = Directive.stop;
    logDirective();
  }

  public void stopped() {
    status = Status.stopped;
    logStatus();
  }

  public void booting() {
    status = Status.booting;
    logStatus();
  }

  public void booted() {
    status = Status.booted;
    logStatus();
  }

  public void starting() {
    status = Status.starting;
    logStatus();
  }

  public void running() {
    status = Status.running;
    logStatus();
  }

  public void reinitializing() {
    status = Status.reinitializing;
    logStatus();
  }

  public void stopping() {
    status = Status.stopping;
    logStatus();
  }

  public boolean equals(SentryRuntime.Directive directive) {
    return this.directive.equals(directive);
  }

  public boolean equals(SentryRuntime.Status status) {
    return this.status.equals(status);
  }

  private void logDirective() {
    logger.info(String.format("%sdirected to %s", name == null ? "" : name + " ", directive.name()));
  }

  private void logStatus() {
    logger.info(String.format("%s%s", name == null ? "" : name + " ", status.name()));
  }

  public enum Directive {
    start, run, reinitialize, stop;
  }

  public enum Status {
    stopped, booting, booted, starting, running, reinitializing, stopping;
  }

}
