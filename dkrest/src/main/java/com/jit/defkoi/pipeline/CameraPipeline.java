package com.jit.defkoi.pipeline;

import com.jit.defkoi.SentryRuntime;
import com.jit.defkoi.ThreadGroupLocal;
import com.jit.defkoi.jpa.PipeConf;
import com.jit.defkoi.service.DeviceApi;
import com.jit.defkoi.service.PipeConfService;
import com.jit.defkoi.service.pref.Config;
import com.jit.defkoi.service.pref.PreferenceService;
import lombok.NoArgsConstructor;
import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Pipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jit.defkoi.SentryRuntime.Directive.start;
import static com.jit.defkoi.SentryRuntime.Directive.stop;
import static com.jit.defkoi.SentryRuntime.Status.reinitializing;

@NoArgsConstructor
public class CameraPipeline extends SentryPipeline {
  private static final Logger logger = LoggerFactory.getLogger(CameraPipeline.class);

  private PreferenceService prefService;
  private PipeConfService pipeConfService;

  // needed to track state changes
  private Config config;
  private PipeConf pipeConf;

  public CameraPipeline(PreferenceService prefService, PipeConfService pipeConfService,
    PipelineContext pipelineContext) {
    this.context = pipelineContext;
    this.pipeConf = context.getPipeConf();
    this.rootName = PipelineContext.rootName(pipeConf);
    this.name = String.format("%s-%d", rootName, ++serial);
    runtime = new SentryRuntime(logger, name);
    runtime.start();
    runtime.starting();

    this.prefService = prefService;
    this.pipeConfService = pipeConfService;
    context.setCameraPipeline(this);
    this.config = prefService.getConfig();
    context.setConfig(config);
  }

  @Override
  public void run() {
    runtime.run();
    new ThreadGroupLocal(context);
    if(context.getRuntime().equals(start))
      new Thread(context).start();
    pipeline = new Pipeline(name);

    try {
      //@formatter:off
      Pipe tee = Pipe.source(pipeline, DeviceApi.valueOf(pipeConf.getDevice().getDeviceApi()), "source")
          .nv(config.isNvCapable() && pipeConf.getNvEnabled()).cap("video/x-raw")
          .width(pipeConf.getCap().getWidth()).height(pipeConf.getCap().getHeight()).set("device", pipeConf.getName())

        // On the Jetson, you'll get an internal data stream error if you don't set the pixel-aspect-ratio.
        // In this instance, it is not necessary to set the width and height (but in some cases it is).
        // On the Jetson, you can't change the format at this stage
        .link(Pipe.convert(pipeline, "sourceConvert").nv(config.isNvCapable() && pipeConf.getNvEnabled()).cap("video/x-raw").aspect("1/1"))
          .width(pipeConf.getCap().getWidth()).height(pipeConf.getCap().getHeight())
        .link(Pipe.lay(pipeline, "tee", "convertTee"));
      //@formatter:on

      boolean csiJpeg =
        DeviceApi.valueOf(pipeConf.getDevice().getDeviceApi()).equals(DeviceApi.csi) && config.isNvCapable();
      if(config.isMotDetectEnabled() && !Boolean.FALSE.equals(pipeConf.getMotDetectEnabled())) {
        MotionConversion motionConversion = context.createMotionConversion();
        context.createMotionDetection();
        //@formatter:off
        tee
          .link(Pipe.lay(pipeline, "queue", "motionAcceptQueue").set("max-size-buffers", 7))
          .link(Pipe.appSink(pipeline, "motionAccept", new FrameCounter(context.getPipelineStats().getMotAccept())));

        // motionConvert won't link to nvjpegenc when specifying ImageFormat, but ImageFormat is needed for rgbFromBgrx
        Pipe motionConvert =
          Pipe.convert(pipeline, "motionConvert").nv(config.isNvCapable() && pipeConf.getNvEnabled())
            .cap("video/x-raw")
            .width(config.getMaxMotDetectWidth()).height(config.getMaxMotDetectHeight()).aspect("1/1");
        Pipe pipe = csiJpeg ?
          motionConvert.link(Pipe.lay(pipeline, "nvjpegenc", "motionJpeg")) :
          motionConvert.format(ImageFormat.gray);
        pipe.link(Pipe.appSink(pipeline, "motionConversion", motionConversion));

        tee
          .link(Pipe.lay(pipeline, "queue", "motionQueue").set("max-size-buffers", config.getQueueMaxSize()))
          .link(motionConvert);
      }
      //@formatter:on

      if(config.isObjDetectEnabled() && !Boolean.FALSE.equals(pipeConf.getObjDetectEnabled())) {
        ObjectConversion objectConversion = context.createObjectConversion();
        context.createObjectDetection();
        //@formatter:off
        tee
          .link(Pipe.lay(pipeline, "queue", "objectAcceptQueue").set("max-size-buffers", 7))
          .link(Pipe.appSink(pipeline, "objectAccept", new FrameCounter(context.getPipelineStats().getObjAccept())));

        // objectConvert won't link to nvjpegenc when specifying ImageFormat, but ImageFormat is needed for rgbFromBgrx
        Pipe objectConvert =
          Pipe.convert(pipeline, "objectConvert").nv(config.isNvCapable() && pipeConf.getNvEnabled())
              .cap("video/x-raw")
              .width(config.getMaxObjDetectWidth()).height(config.getMaxObjDetectHeight()).aspect("1/1");
        Pipe pipe = csiJpeg ?
          objectConvert.link(Pipe.lay(pipeline, "nvjpegenc", "objectJpeg")) :
          objectConvert.format(ImageFormat.color);
        pipe.link(Pipe.appSink(pipeline, "objectConversion", objectConversion));

        tee
          .link(Pipe.lay(pipeline, "queue", "objectQueue").set("max-size-buffers", config.getQueueMaxSize()))
          .link(objectConvert);

        if(config.isObjectRtspEnabled())
          context.createPublishPipeline();
      }

      if(config.isTapLiveEnabled() && !config.isDockerized()) {
        //@formatter:off
        tee
          .link(Pipe.lay(pipeline, "queue", "liveQueue").set("max-size-buffers", config.getQueueMaxSize()))
          .link(Pipe.convert(pipeline, "liveConvert").nv(config.isNvCapable() && pipeConf.getNvEnabled()))
          .link(Pipe.lay(pipeline, "xvimagesink", "detectTap"));
      }
      //@formatter:on

      if(config.isLiveRtspEnabled()) {
        String url = String.format("%slive/%s", config.getRtspProxyUrl(), context.getRootName().replaceAll(" ", ""));
        logger.info("Publishing live feed to " + url);
        int protos =
          config.isDockerized() ? RtspProtocol.tcp.getBit() : RtspProtocol.bitOr(RtspProtocol.udp, RtspProtocol.tcp);
        //@formatter:off
        tee
          .link(Pipe.lay(pipeline, "queue", "liveRtspQueue").set("max-size-buffers", config.getQueueMaxSize()))
          .link(Pipe.convert(pipeline, "liveRtspConvert").nv(config.isNvCapable() && pipeConf.getNvEnabled()))
          .link(Pipe.lay(pipeline, "rtspclientsink", "liveRtsp")
            .set("location", url).set("protocols", protos));
      }
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
      context.killCameraPipeline();
    }
  }

  protected void reinit() {
    // capture current state before refreshing config
    String rtspProxyUrl = config.getRtspProxyUrl();
    config = prefService.getConfig();
    context.setConfig(config);
    if(runtime.equals(reinitializing))
      return;

    try {
      if(runtime.equals(stop))
        throw new DirtyConfigException("Cannot reinit stopped pipeline, so recreating");
      runtime.reinitialize();
      runtime.reinitializing();
      PipeConf optSrc = pipeConfService.save(context.getPipeConf().optimizeResolution(config));
      if(!pipeConf.getCap().equals(optSrc.getCap()))
        throw new DirtyConfigException(
          "PipeConf's selected device capability has changed, so we must rebuild the pipeline");
      if(!pipeConf.getNvEnabled().equals(optSrc.getNvEnabled()))
        throw new DirtyConfigException(
          String.format("PipeConf's nvEnabled changed from %s to %s, so we must rebuild the pipeline",
            pipeConf.getNvEnabled(), optSrc.getNvEnabled()));
      pipeConf = optSrc;
      context.setPipeConf(pipeConf);

      // TODO: can we add a tee branch after pipeline started playing? probably not
      if(config.isMotDetectEnabled() && !Boolean.FALSE.equals(pipeConf.getMotDetectEnabled())) {
        if(pipeline.getElementByName("motionQueue") == null)
          throw new DirtyConfigException(
            "Motion detection branch was not previously built, so we must rebuild the pipeline");
        context.getMotionConversion().reinit();
      }
      if(config.isObjDetectEnabled() && !Boolean.FALSE.equals(pipeConf.getObjDetectEnabled())) {
        if(pipeline.getElementByName("objectQueue") == null)
          throw new DirtyConfigException(
            "Object detection branch was not previously built, so we must rebuild the pipeline");
        context.getObjectConversion().reinit();
      }
      if(config.isTapLiveEnabled()) {
        if(pipeline.getElementByName("liveQueue") == null)
          throw new DirtyConfigException("tapLiveEnabled has changed, so we must rebuild the pipeline");
      }
      if(config.isLiveRtspEnabled()) {
        if(pipeline.getElementByName("liveRtspQueue") == null)
          throw new DirtyConfigException(
            "RTSP live feed publishing branch was not previously built, so we must rebuild the pipeline");
        if(!rtspProxyUrl.equals(config.getRtspProxyUrl()))
          throw new DirtyConfigException("RTSP base URL has changed, so we must rebuild the pipeline");
      }

      if(config.isObjectRtspEnabled() && !Boolean.FALSE.equals(pipeConf.getObjDetectEnabled()))
        context.createPublishPipeline();
      runtime.run();
      runtime.running();
    } catch(DirtyConfigException e) {
      logger.info(e.getMessage());
      stop();
      new Thread(new CameraPipeline(prefService, pipeConfService, context)).start();
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
    if(pipeline != null) {
      if(pipeline.getElementByName("liveRtsp") != null)
        // TODO: this doesn't seem to actually stop the element (trying to avoid errors 9 and 10 when stopping pipeline)
        pipeline.getElementByName("liveRtsp").stop();
      pipeline.stop();
    }
    pipeline = null;
    runtime.stopped();
  }

}
