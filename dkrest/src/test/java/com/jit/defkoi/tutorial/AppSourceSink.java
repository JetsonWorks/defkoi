package com.jit.defkoi.tutorial;

import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.AppSrc;
import org.freedesktop.gstreamer.lowlevel.MainLoop;

import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Semaphore;

// basic tutorial 8
public class AppSourceSink {

  private static final int chunkSize = 10240; // 1/4 second of audio
  private static final int sampleRate = 44100;
  private static final int audioQueueSize = 1024; // generate audio in advance

  private Pipeline pipeline;
  private Semaphore gotEOSPipeline = new Semaphore(0);
  private final MainLoop loop = new MainLoop();

  private AppSrc appSource;
  private Element tee, audioQueue, audioConvert1, audioResample, audioSink;
  private Element videoQueue, audioConvert2, visual, videoConvert, videoSink;
  private AppSink appSink;

  private Element appQueue;
  private AppSrcListener appSrcListener;

  private Pad teeAudio, teeVideo, teeApp;
  private Pad queueAudio, queueVideo, queueApp;

  public static void main(String[] args) {
    Utils.configurePaths();
    Gst.init(Version.BASELINE, "BasicPipeline", args);
    AppSourceSink app = new AppSourceSink();
    app.run();
  }

  AppSourceSink() {
    try {
      pipeline = new Pipeline("test-pipeline");
      appSource = (AppSrc)ElementFactory.make("appsrc", "audio_source");
      tee = ElementFactory.make("tee", "tee");
      audioQueue = ElementFactory.make("queue", "audio_queue");
      audioConvert1 = ElementFactory.make("audioconvert", "audio_convert");
      audioResample = ElementFactory.make("audioresample", "audio_resample");
      audioSink = ElementFactory.make("autoaudiosink", "audio_sink");

      videoQueue = ElementFactory.make("queue", "video_queue");
      audioConvert2 = ElementFactory.make("audioconvert", "audio_convert2");
      visual = ElementFactory.make("wavescope", "visual");
      videoConvert = ElementFactory.make("videoconvert", "csp");
      videoSink = ElementFactory.make("autovideosink", "video_sink");

      appQueue = ElementFactory.make("queue", "app_queue");
      appSink = (AppSink)ElementFactory.make("appsink", "app_sink");
    } catch(Exception e) {
      System.err.println("Not all elements could be created: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    try {
      visual.set("shader", 0);
      visual.set("style", 1);

      // Configure appsrc
      Caps audioCaps = new Caps(
        "audio/x-raw, format=(string)F32LE, layout=(string)interleaved, " + "rate=(int)" + sampleRate
          + ", channels=(int)1;");
      appSource.setCaps(audioCaps);
      // https://gstreamer.freedesktop.org/documentation/gstreamer/gstformat.html?gi-language=c#GST_FORMAT_TIME
      appSource.set("format", Format.TIME);

      appSrcListener = new AppSrcListener(appSource, loop);
      appSource.set("emit-signals", true);
      appSource.connect((AppSrc.NEED_DATA)appSrcListener);
      appSource.connect((AppSrc.ENOUGH_DATA)appSrcListener);

      // Configure appsink
      appSink.setCaps(audioCaps);
      appSink.set("emit-signals", true);
      appSink.connect(new AppSinkListener());

      pipeline.addMany(appSource, tee, audioQueue, audioConvert1, audioResample, audioSink);
      pipeline.addMany(videoQueue, audioConvert2, visual, videoConvert, videoSink);
      pipeline.addMany(appQueue, appSink);

      // Link all elements that can be automatically linked because they have "Always" pads
      pipeline.linkMany(appSource, tee);
      pipeline.linkMany(audioQueue, audioConvert1, audioResample, audioSink);
      pipeline.linkMany(videoQueue, audioConvert2, visual, videoConvert, videoSink);
      pipeline.linkMany(appQueue, appSink);

      // Manually link the Tee elements, which have "Request" pads
      teeAudio = tee.getRequestPad("src_%u");
      System.out.println(String.format("Obtained request pad %s for audio branch.", teeAudio.getName()));
      queueAudio = audioQueue.getStaticPad("sink");
      teeAudio.link(queueAudio);

      teeVideo = tee.getRequestPad("src_%u");
      System.out.println(String.format("Obtained request pad %s for video branch.", teeVideo.getName()));
      queueVideo = videoQueue.getStaticPad("sink");
      teeVideo.link(queueVideo);

      teeApp = tee.getRequestPad("src_%u");
      System.out.println(String.format("Obtained request pad %s for app branch.", teeApp.getName()));
      queueApp = appQueue.getStaticPad("sink");
      teeApp.link(queueApp);

    } catch(Exception e) {
      System.err.println("Elements could not be linked: " + e);
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void run() {
    pipeline.getBus().connect((Bus.EOS)(source) -> {
      System.out.println("Received EOS on the pipeline");
      loop.quit();
    });
    pipeline.getBus().connect((Bus.ERROR)(source, code, message) -> {
      System.err.println(String.format("'%s' error %d: %s", source.getName(), code, message));
      loop.quit();
      stop(1);
    });
    pipeline.getBus().connect((Bus.WARNING)(source, code, message) -> {
      System.err.println(String.format("'%s' warning %d: %s", source.getName(), code, message));
    });
    pipeline.getBus().connect((Bus.INFO)(source, code, message) -> {
      System.err.println(String.format("'%s' info %d: %s", source.getName(), code, message));
    });
    pipeline.getBus().connect((Bus.STATE_CHANGED)(source, old, current, pending) -> {
      System.out.println(String.format("'%s' changed state from %s to %s", source.getName(), old, current));
    });

    pipeline.play();
    loop.run();
    stop(0);
  }

  private void stop(int status) {
    pipeline.stop();
    Gst.deinit();
    Gst.quit();
    System.exit(status);
  }

  private class AppSrcListener implements AppSrc.NEED_DATA, AppSrc.ENOUGH_DATA, Runnable {
    private AppSrc appSrc;
    private int samplesPerBuffer = Math.round(chunkSize / 4); // ints and floats are 4 bytes
    private int totalSamples;
    private float a, b, c, d;

    private ArrayBlockingQueue<Buffer> queue = new ArrayBlockingQueue<>(audioQueueSize);
    private boolean feedMe;

    public AppSrcListener(AppSrc src, MainLoop mainLoop) {
      appSrc = src;
      mainLoop.invokeLater(this);
      System.out.println("Queueing initial buffer");
      while(queue.size() < audioQueueSize)
        generateData();
      System.out.println("Done queueing initial buffer");
    }

    public void generateData() {
      Buffer buffer = new Buffer(chunkSize);
      buffer.setPresentationTimestamp(Math.round((float)totalSamples * 1000000000 / sampleRate));
      buffer.setDuration(samplesPerBuffer * 1000000000);

      ByteBuffer buf = buffer.map(true);
      c += d;
      d -= (c / 1000);
      float freq = 1100 + 1000 * d;
      for(int i = 0; i < samplesPerBuffer; i++) {
        a += b;
        b -= a / freq;
        buf.putFloat(500 * a);
      }
      buffer.unmap();
      totalSamples += samplesPerBuffer;
      queue.add(buffer);
    }

    @Override
    public void run() {
      while(true) {
        if(queue.size() < audioQueueSize)
          generateData();
        try {
          Thread.sleep(10);
        } catch(InterruptedException e) {
        }
      }
    }

    @Override
    public void needData(AppSrc elem, int size) {
      System.out.println("needData (" + size + ")");
      feedMe = true;
      while(feedMe) {
        if(queue.size() < 2) {
          System.out.println("generating data on demand");
          generateData();
        }
        try {
          appSrc.pushBuffer(queue.take());
        } catch(InterruptedException e) {
          System.err.println("Interrupted while taking buffer from queue: " + e);
          e.printStackTrace();
        }
      }
    }

    @Override
    public void enoughData(AppSrc appSrc) {
      System.out.println(String.format("enoughData; totalSamples=%.2fM", (float)totalSamples / 1048576));
      feedMe = false;
    }
  }

  private static class AppSinkListener implements AppSink.NEW_SAMPLE {
    @Override
    public FlowReturn newSample(AppSink appSink) {
      System.out.print("*");
      Sample sample = appSink.pullSample();
      sample.dispose();
      return FlowReturn.OK;
    }
  }

}
