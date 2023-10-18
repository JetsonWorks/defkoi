package com.jit.defkoi.pipeline;

import com.jit.defkoi.SentryRuntime;
import com.jit.defkoi.ThreadGroupLocal;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.elements.AppSrc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jit.defkoi.SentryRuntime.Directive.stop;
import static com.jit.defkoi.pipeline.PipelineContext.publishIdlePeriod;

/**
 * If predicted image publishing is enabled, pulls Buffers from the predicted queue and places them on the pipeline.
 * Runs in a separate thread to avoid blocking the CameraPipeline thread.
 */
public class ObjectPublisher implements Runnable, AppSrc.NEED_DATA, AppSrc.ENOUGH_DATA {

  private static final Logger logger = LoggerFactory.getLogger(ObjectPublisher.class);

  private SentryRuntime runtime = new SentryRuntime(logger);
  protected AppSrc appSrc;

  private PipelineContext context;
  private boolean srcReady;

  public ObjectPublisher() {
    this.context = new ThreadGroupLocal<PipelineContext>().getValue();
    runtime.setName(context.getPublishPipeline().getName() + "." + this.getClass().getSimpleName());
    runtime.start();
    runtime.starting();
    new Thread(this).start();
  }

  @Override
  public void run() {
    runtime.run();
    runtime.running();
    ConcurrentLinkedQueue<Buffer> predicted = context.getPredicted();
    Metric cost = context.getPipelineStats().getObjPubProcess();
    while(!runtime.equals(stop)) {
      if(!predicted.isEmpty()) {
        if(srcReady) {
          try {
            Buffer buffer = predicted.remove();
            cost.startClock();
            appSrc.pushBuffer(buffer);
            continue;
          } catch(Exception e) {
            logger.error(e.toString(), e);
          } finally {
            cost.stopClock();
          }
        } else {
          configureAppSrc();
        }
      }
      try {
        Thread.sleep(publishIdlePeriod);
      } catch(InterruptedException e) {
        logger.error(e.toString(), e);
      }
    }
    runtime.stopped();
  }

  protected void configureAppSrc() {
    if(appSrc != null)
      srcReady = true;
  }

  public void stop() {
    if(runtime.equals(stop))
      return;
    runtime.stop();
  }

  public void needData(AppSrc appSrc, int size) {
    if(appSrc != null)
      this.appSrc = appSrc;
    logger.trace(String.format("'%s' received needData (%d)", appSrc.getName(), size));
  }

  public void enoughData(AppSrc appSrc) {
    logger.trace(String.format("'%s' received enough data", appSrc.getName()));
  }

}
