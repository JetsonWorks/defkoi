package com.jit.defkoi.config;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class LogbackConfig {

  private static final Logger logger = LoggerFactory.getLogger(LogbackConfig.class);

  @Value("${logbackLocation}")
  private String location;

  @Bean(name = "logbackContextConfig")
  public LoggerContext logbackConfig() {
    LoggerContext context = (LoggerContext)LoggerFactory.getILoggerFactory();

    if(!new File(location).exists()) {
      logger.warn("File '" + location + "' does not exist, using default logback configuration");
      return context;
    }

    try {
      JoranConfigurator conf = new JoranConfigurator();
      conf.setContext(context);
      context.reset();
      conf.doConfigure(location);
    } catch(JoranException je) {
      // StatusPrinter will handle this
    }
    StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    return context;
  }

}
