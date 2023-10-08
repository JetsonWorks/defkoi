package com.jit.defkoi.pipeline;

import com.jit.defkoi.SentryRuntime;
import com.jit.defkoi.ThreadGroupLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jit.defkoi.SentryRuntime.Directive.stop;

/**
 * Pulls images from the motionConverted queue, analyzes differences to detect motion, and updates the CameraPipeline's
 * state accordingly. Runs in a separate thread to avoid blocking the CameraPipeline thread.
 */
public class MotionDetection implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(MotionDetection.class);

  private SentryRuntime runtime = new SentryRuntime(logger);
  private PipelineContext context;
  private BufferedImage prev;

  public MotionDetection() {
    context = new ThreadGroupLocal<PipelineContext>().getValue();
    runtime.setName(context.getCameraPipeline().getName() + "." + this.getClass().getSimpleName());
    runtime.start();
    runtime.starting();
    new Thread(this).start();
  }

  @Override
  public void run() {
    runtime.run();
    runtime.running();
    ConcurrentLinkedQueue<BufferedImage> converted = context.getMotionConverted();
    Metric cost = context.getPipelineStats().getMotProcess();
    while(!runtime.equals(stop)) {
      if(!converted.isEmpty()) {
        try {
          cost.startClock();
          BufferedImage curr;
          for(curr = converted.remove(); converted.size() > 0; curr = converted.remove())
            logger.trace(MotionDetection.class.getSimpleName() + " cinching converted queue");

          // TODO: do motion detection
          Metric numDetected = context.getPipelineStats().getMotFramesDetected();

          prev = curr;
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

}
