package com.jit.defkoi.pipeline;

import ai.djl.modality.cv.Image;
import com.jit.defkoi.Detector;
import com.jit.defkoi.SentryRuntime;
import com.jit.defkoi.ThreadGroupLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jit.defkoi.SentryRuntime.Directive.stop;
import static com.jit.defkoi.pipeline.ImageUtils.cvImageToBufferPng;

/**
 * If the CameraPipeline's state calls for object detection, pulls images from the objectConverted queue, runs them
 * through the Detector, and updates the CameraPipeline's state accordingly. Runs in a separate thread to avoid blocking
 * the CameraPipeline thread.
 */
public class ObjectDetection implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(ObjectDetection.class);

  private SentryRuntime runtime = new SentryRuntime(logger);
  private PipelineContext context;
  private String cameraPipelineName;
  private int sequence = 0;

  public ObjectDetection() {
    context = new ThreadGroupLocal<PipelineContext>().getValue();
    cameraPipelineName = context.getCameraPipeline().getName();
    runtime.setName(cameraPipelineName + "." + this.getClass().getSimpleName());
    runtime.start();
    runtime.starting();
    new Thread(this).start();
  }

  @Override
  public void run() {
    runtime.run();
    runtime.running();
    ConcurrentLinkedQueue<BufferedImage> converted = context.getObjectConverted();
    Metric cost = context.getPipelineStats().getObjProcess();
    Metric numDetected = context.getPipelineStats().getObjFramesDetected();
    while(!runtime.equals(stop)) {
      if(!converted.isEmpty()) {
        try {
          cost.startClock();
          BufferedImage image;
          for(image = converted.remove(); converted.size() > 0; image = converted.remove())
            logger.trace(ObjectDetection.class.getSimpleName() + " cinching converted queue");

          // TODO: each call to detector.predict(image) should be made from a separate thread, and then ObjectDetection may not need to be running separately from CameraPipeline
          // TODO: there is a possibility of overloading the GPU - we just don't know what it's capable of

          Detector.CvImageDetection detection = context.getDetector().predict(image);
          if(detection.getObjects().getNumberOfObjects() > 0)
            numDetected.increment();
          if(context.getConfig().isSaveBoundingBoxImage())
            saveImage(detection.getImage());

          if(context.getConfig().isObjectRtspEnabled())
            try {
              context.getPredicted().add(cvImageToBufferPng(detection.getImage()));
            } catch(Exception e) {
              logger.error(e.toString(), e);
            }

        } catch(Exception e) {
          logger.error(e.toString(), e);
        } finally {
          cost.stopClock();
        }
      } else {
        try {
          Thread.sleep(PipelineContext.detectionIdlePeriod);
        } catch(InterruptedException e) {
          logger.error(e.toString(), e);
        }
      }
    }
    runtime.stopped();
  }

  public void stop() {
    if(runtime.equals(stop))
      return;
    runtime.stop();
  }

  private void saveImage(Image img) throws IOException {
    File outputDir = new File(new File(context.getConfig().getMediaDir()), cameraPipelineName);
    outputDir.mkdirs();
    File imagePath = new File(outputDir, String.format("%04d.png", sequence));
    sequence++;
    img.save(new FileOutputStream(imagePath), "png");
    logger.info("Detection result image has been saved in: {}", imagePath);
  }

}
