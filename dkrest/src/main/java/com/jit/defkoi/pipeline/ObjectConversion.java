package com.jit.defkoi.pipeline;

import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;

/**
 * Just pulls images from incoming Samples, converts them, and places them on the objectConverted queue. Runs in the
 * CameraPipeline's thread.
 */
public class ObjectConversion extends MotionConversion implements AppSink.NEW_SAMPLE {
  private static final Logger logger = LoggerFactory.getLogger(ObjectConversion.class);

  /**
   * Creates images from samples and runs them through Detector. Using the Bus, the AppSink could send an ErrorMessage,
   * but it's not clear how to instantiate Messages. We can't throw Exceptions that aren't on the newSample() signature.
   * Throwing a RuntimeException here would result in:
   * @formatter:off
   * 2022-07-29 15:57:21,408 WARN  [GstBus] com.sun.jna.Native - JNA: Callback org.freedesktop.gstreamer.elements.AppSink$2@152dadd0 threw the following exception
   * @formatter:on
   * @param appSink AppSink
   * @return
   */
  @Override
  public FlowReturn newSample(AppSink appSink) {
    logger.trace(ObjectConversion.class.getSimpleName() + " received new sample");
    Metric cost = context.getPipelineStats().getObjConversion();
    if(!context.getConfig().isObjDetectEnabled() || !context.getPipeConf().getObjDetectEnabled()) {
      Sample sample = appSink.pullSample();
      sample.dispose();
      positiveResults.add(1);
      return FlowReturn.OK;
    }

    BufferedImage image = pullSampleBufferImage(appSink, cost);
    if(image == null) {
      positiveResults.add(0);
      if(getSuccessRate() <= successThreshold) {
        logger.info(String.format("Success rate (%f) <= threshold (%f)", getSuccessRate(), successThreshold));
        context.killCameraPipeline();
      }
      return FlowReturn.ERROR;
    }
    context.getObjectConverted().add(image);
    positiveResults.add(1);
    return FlowReturn.OK;
  }

}

