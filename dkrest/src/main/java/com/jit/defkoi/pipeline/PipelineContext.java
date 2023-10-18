package com.jit.defkoi.pipeline;

import com.jit.defkoi.Detector;
import com.jit.defkoi.Sentry;
import com.jit.defkoi.SentryRuntime;
import com.jit.defkoi.jpa.PipeConf;
import com.jit.defkoi.jpa.Stats;
import com.jit.defkoi.service.pref.Config;
import lombok.Data;
import org.freedesktop.gstreamer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.awt.image.BufferedImage;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import static com.jit.defkoi.SentryRuntime.Directive.reinitialize;
import static com.jit.defkoi.SentryRuntime.Directive.stop;

@Data
public class PipelineContext implements Runnable {

  private static final Logger logger = LoggerFactory.getLogger(PipelineContext.class);

  public static String rootName(PipeConf pipeConf) {
    return pipeConf.getDevice().getDisplayName();
  }

  protected static final int reinitIdlePeriod = 500;
  protected static final int detectionIdlePeriod = 10;
  protected static final int publishIdlePeriod = 50;

  private static Integer statsInterval = 1;
  private static Integer statsWindow = 60;
  private static Timer statsTimer = new Timer();

  private SentryRuntime runtime;
  private PipeConf pipeConf;
  private Authentication serviceToken;
  private CameraPipeline cameraPipeline;
  private PublishPipeline publishPipeline;
  private Config config;

  private PipelineStats pipelineStats;
  private Stats stats;
  private BoundedQueue<Stats> statsHistory = new BoundedQueue<>(statsWindow / statsInterval);

  private MotionConversion motionConversion;
  private ConcurrentLinkedQueue<BufferedImage> motionConverted = new BoundedQueue<>(1);
  private MotionDetection motionDetection;

  private ObjectConversion objectConversion;
  private ConcurrentLinkedQueue<BufferedImage> objectConverted = new BoundedQueue<>(1);
  private ObjectDetection objectDetection;
  private ConcurrentLinkedQueue<Buffer> predicted = new BoundedQueue<>(5);
  private ObjectPublisher objectPublisher;

  public PipelineContext(PipeConf pipeConf, Authentication token) {
    this.pipeConf = pipeConf;
    runtime = new SentryRuntime(logger, getRootName());
    runtime.start();
    runtime.starting();
    this.serviceToken = token;
    pipelineStats = new PipelineStats(getRootName());
    statsTimer.scheduleAtFixedRate(new StatsCollector(), statsInterval * 1000, statsInterval * 1000);
  }

  public String getRootName() {
    return rootName(pipeConf);
  }

  public Detector getDetector() {
    return Sentry.getDetector();
  }

  public void run() {
    runtime.run();
    runtime.running();
    SecurityContextHolder.getContext().setAuthentication(serviceToken);
    while(!runtime.equals(stop)) {
      if(runtime.equals(reinitialize)) {
        if(cameraPipeline != null)
          cameraPipeline.reinit();
        if(publishPipeline != null)
          publishPipeline.reinit();
        runtime.run();
        runtime.running();
      }
      try {
        Thread.sleep(reinitIdlePeriod);
      } catch(InterruptedException e) {
        logger.error(e.toString(), e);
      }
    }
    runtime.stopped();
  }

  private void calcStats() {
    if(pipelineStats == null)
      return;
    stats = new Stats(pipelineStats);
    statsHistory.add(stats);
  }

  private class StatsCollector extends TimerTask {
    public void run() {
      if(cameraPipeline != null && !cameraPipeline.getRuntime().equals(stop))
        calcStats();
    }
  }

  public void reinit(PipeConf pipeConf) {
    runtime.reinitialize();
    runtime.reinitializing();
    this.pipeConf = pipeConf;
  }

  public void killCameraPipeline() {
    if(cameraPipeline == null)
      return;
    cameraPipeline.stop();
  }

  public void killPublishPipeline() {
    if(publishPipeline == null)
      return;
    publishPipeline.stop();
  }

  public void stop() {
    runtime.stop();
    runtime.stopping();
    if(cameraPipeline != null)
      cameraPipeline.stop();
    if(publishPipeline != null)
      publishPipeline.stop();
  }

  public void debugToDotFile(String stage) {
    if(cameraPipeline != null)
      cameraPipeline.debugToDot(stage);
    if(publishPipeline != null)
      publishPipeline.debugToDot(stage);
  }

  public MotionConversion createMotionConversion() {
    if(motionConversion == null)
      motionConversion = new MotionConversion();
    return motionConversion;
  }

  public MotionDetection createMotionDetection() {
    if(motionDetection == null)
      motionDetection = new MotionDetection();
    return motionDetection;
  }

  public ObjectConversion createObjectConversion() {
    if(objectConversion == null)
      objectConversion = new ObjectConversion();
    return objectConversion;
  }

  public ObjectDetection createObjectDetection() {
    if(objectDetection == null)
      objectDetection = new ObjectDetection();
    return objectDetection;
  }

  public PublishPipeline createPublishPipeline() {
    if(publishPipeline == null)
      publishPipeline = new PublishPipeline();
    return publishPipeline;
  }

  public ObjectPublisher createObjectPublisher() {
    if(objectPublisher == null)
      objectPublisher = new ObjectPublisher();
    return objectPublisher;
  }

}
