package com.jit.defkoi.tutorial;

import org.freedesktop.gstreamer.*;

import java.util.concurrent.Semaphore;

// basic tutorial 7
public class Multithreading {
  private Pipeline pipeline;
  private Element audioSource, tee, audioQueue, audioConvert, audioResample, audioSink;
  private Element videoQueue, visual, videoConvert, videoSink;
  private Semaphore gotEOSPipeline = new Semaphore(0);
  private Pad teeAudio, teeVideo;
  private Pad queueAudio, queueVideo;

  public static void main(String[] args) {
    Utils.configurePaths();
    Gst.init(Version.BASELINE, "BasicPipeline", args);
    Multithreading app = new Multithreading();
    app.run();
  }

  Multithreading() {
    try {
      pipeline = new Pipeline("test-pipeline");
      audioSource = ElementFactory.make("audiotestsrc", "audio_source");
      tee = ElementFactory.make("tee", "tee");
      audioQueue = ElementFactory.make("queue", "audio_queue");
      audioConvert = ElementFactory.make("audioconvert", "audio_convert");
      audioResample = ElementFactory.make("audioresample", "audio_resample");
      audioSink = ElementFactory.make("autoaudiosink", "audio_sink");
      videoQueue = ElementFactory.make("queue", "video_queue");
      visual = ElementFactory.make("wavescope", "visual");
      videoConvert = ElementFactory.make("videoconvert", "csp");
      videoSink = ElementFactory.make("autovideosink", "video_sink");
    } catch(Exception e) {
      System.err.println("Not all elements could be created: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    try {
      audioSource.set("freq", 215.0f);
      visual.set("shader", 0);
      visual.set("style", 1);
      pipeline.addMany(audioSource, tee, audioQueue, audioConvert, audioResample, audioSink, videoQueue, visual,
        videoConvert, videoSink);

      /* Link all elements that can be automatically linked because they have "Always" pads */
      pipeline.linkMany(audioSource, tee);
      pipeline.linkMany(audioQueue, audioConvert, audioResample, audioSink);
      pipeline.linkMany(videoQueue, visual, videoConvert, videoSink);

      /* Manually link the Tee elements, which have "Request" pads */
      teeAudio = tee.getRequestPad("src_%u");
      System.out.println(String.format("Obtained request pad %s for audio branch.", teeAudio.getName()));
      queueAudio = audioQueue.getStaticPad("sink");
      teeAudio.link(queueAudio);

      teeVideo = tee.getRequestPad("src_%u");
      System.out.println(String.format("Obtained request pad %s for video branch.", teeVideo.getName()));
      queueVideo = videoQueue.getStaticPad("sink");
      teeVideo.link(queueVideo);
    } catch(Exception e) {
      System.err.println("Elements could not be linked: " + e);
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void run() {
    pipeline.getBus().connect((Bus.EOS)(source) -> {
      System.out.println("Received EOS on the pipeline");
      gotEOSPipeline.release();
    });
    pipeline.getBus().connect((Bus.ERROR)(source, code, message) -> {
      System.err.println(String.format("'%s' error %d: %s", source.getName(), code, message));
      gotEOSPipeline.release();
    });
    pipeline.getBus().connect((Bus.STATE_CHANGED)(source, old, current, pending) -> {
      System.out.println(String.format("'%s' changed state from %s to %s", source.getName(), old, current));
    });
    pipeline.play();

    try {
      gotEOSPipeline.acquire(1);
      pipeline.stop();
      Gst.deinit();
      Gst.quit();
    } catch(InterruptedException e) {
      System.err.println(e);
    }
  }

}
