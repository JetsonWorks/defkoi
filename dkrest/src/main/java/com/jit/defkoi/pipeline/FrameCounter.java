package com.jit.defkoi.pipeline;

import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Just increments the count of captured frames. */
public class FrameCounter implements AppSink.NEW_SAMPLE {
  private static final Logger logger = LoggerFactory.getLogger(FrameCounter.class);

  protected Metric metric;

  public FrameCounter(Metric metric) {
    this.metric = metric;
  }

  public FlowReturn newSample(AppSink appSink) {
    logger.trace(FrameCounter.class.getSimpleName() + " received new sample");
    metric.increment();
    Sample sample = appSink.pullSample();
    sample.dispose();
    return FlowReturn.OK;
  }

}

