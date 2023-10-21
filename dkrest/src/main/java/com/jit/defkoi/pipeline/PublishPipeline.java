package com.jit.defkoi.pipeline;

import com.jit.defkoi.SentryRuntime;
import com.jit.defkoi.ThreadGroupLocal;
import com.jit.defkoi.service.pref.Config;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jit.defkoi.SentryRuntime.Directive.stop;
import static com.jit.defkoi.SentryRuntime.Status.reinitializing;

public class PublishPipeline extends SentryPipeline {
  private static final Logger logger = LoggerFactory.getLogger(PublishPipeline.class);

  // needed to track state changes
  private Config config;

  public PublishPipeline() {
    context = new ThreadGroupLocal<PipelineContext>().getValue();
    config = context.getConfig();
    rootName = context.getRootName();
    name = String.format("%s-objectPublish-%d", rootName, ++serial);
    runtime = new SentryRuntime(logger, name);
    runtime.start();
    runtime.starting();
    new Thread(this).start();
  }

  public void run() {
    runtime.run();
    logger.info("Creating new pipeline: " + name);
    pipeline = new Pipeline(name);
    config = context.getConfig();

    try {
      String url = String.format("%sobject/%s", config.getRtspProxyUrl(), rootName.replaceAll(" ", ""));
      ObjectPublisher objectPublisher = context.createObjectPublisher();
      logger.info("Publishing object images to " + url);
      //@formatter:off
      Pipe tee = Pipe.appSource(pipeline, "objectPublisher")
        .needDataListener(objectPublisher).enoughDataListener(objectPublisher)
        .link(Pipe.lay(pipeline, "queue", "objectPngQueue"))
        .link(Pipe.lay(pipeline, "pngdec", "objectPngDec"))
        .link(Pipe.convert(pipeline, "objectRtspConvert"))
        .link(Pipe.lay(pipeline, "tee", "objectTee"));

      tee
        .link(Pipe.appSink(pipeline, "objectRtspAccept", new FrameCounter(context.getPipelineStats().getObjPubAccept())));
      tee
        .link(Pipe.lay(pipeline, "queue", "objectRtspQueue").set("max-size-buffers", config.getQueueMaxSize()))
        .link(Pipe.lay(pipeline, "rtspclientsink", "objectRtsp")
          .set("location", url).set("protocols", RtspProtocol.bitOr(RtspProtocol.tcp)));
      //@formatter:on

      pipeline.getBus().connect((Bus.EOS)this::endOfStream);
      pipeline.getBus().connect((Bus.ERROR)this::errorMessage);
      pipeline.getBus().connect((Bus.STATE_CHANGED)this::stateChanged);
      pipeline.getBus().connect((Bus.WARNING)this::warningMessage);
      pipeline.getBus().connect((Bus.INFO)this::infoMessage);
      pipeline.getBus().connect((Bus.MESSAGE)this::busMessage);

      if(config.isDebug())
        debugToDot("created");

      pipeline.play();
      runtime.running();

      if(config.isDebug())
        debugToDot("playing");
    } catch(Exception e) {
      logger.error(e.toString(), e);
      if(config.isDebug())
        debugToDot("failed");
      context.killPublishPipeline();
    }
  }

  protected void reinit() {
    if(runtime.equals(reinitializing))
      return;
    runtime.reinitialize();
    runtime.reinitializing();
    String rtspProxyUrl = config.getRtspProxyUrl();
    config = context.getConfig();
    try {
      if(!rtspProxyUrl.equals(config.getRtspProxyUrl()))
        throw new DirtyConfigException("RTSP base URL has changed, so we must rebuild the pipeline");
      runtime.run();
      runtime.running();
    } catch(DirtyConfigException e) {
      logger.info(e.getMessage());
      stop();
      new Thread(new PublishPipeline()).start();
    }
  }

  public void stop() {
    if(runtime.equals(stop))
      return;
    runtime.stop();
    runtime.stopping();
    logger.info("Stop called, so shutting down GST pipeline for " + name);
    if(context.getConfig().isDebug()) {
      debugToDot("final");
    }
    if(pipeline != null)
      pipeline.stop();
    pipeline = null;
    runtime.stopped();
  }

}
