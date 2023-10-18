package com.jit.defkoi.pipeline;

import com.jit.defkoi.SentryRuntime;
import lombok.Getter;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

public abstract class SentryPipeline implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(SentryPipeline.class);
  protected static int serial = 0;

  @Getter
  protected SentryRuntime runtime;
  protected PipelineContext context;
  @Getter
  protected String rootName;
  @Getter
  protected String name;
  protected Pipeline pipeline;

  public abstract void run();

  protected abstract void reinit();

  public void stop(int status) {
    logger.info("Stop called from:", new Throwable());
    context.killCameraPipeline();
    System.exit(status);
  }

  public void debugToDot(String stage) {
    if(pipeline == null)
      return;
    if(System.getenv("GST_DEBUG_DUMP_DOT_DIR") == null)
      return;
    String name = pipeline.getName() + "-" + stage;
    EnumSet details = EnumSet.allOf(Bin.DebugGraphDetails.class);
    pipeline.debugToDotFile(details, name);
    logger.debug("Pipeline status saved to DOT file: " + System.getenv("GST_DEBUG_DUMP_DOT_DIR") + "/" + name);
  }

  public void endOfStream(GstObject source) {
    logger.info(String.format("Pipeline %s '%s' received EOS on the pipeline", name, source.getName()));
    context.killCameraPipeline();
  }

  public void errorMessage(GstObject source, int code, String message) {
    logger.error(String.format("Pipeline %s '%s' error %d: %s", name, source.getName(), code, message));
    context.killCameraPipeline();
  }

  public void stateChanged(GstObject source, State old, State current, State pending) {
    logger.debug(String.format("Pipeline %s '%s' changed state from %s to %s", name, source.getName(), old, current));
  }

  public void warningMessage(GstObject source, int code, String message) {
    logger.warn(String.format("Pipeline %s '%s' warning %d: %s", name, source.getName(), code, message));
  }

  public void infoMessage(GstObject source, int code, String message) {
    logger.info(String.format("Pipeline %s '%s' info %d: %s", name, source.getName(), code, message));
  }

  public void busMessage(Bus bus, Message message) {
    logger.trace(String.format("Pipeline %s '%s' message %s", name, bus.getName(), message.getType()));
  }

}
