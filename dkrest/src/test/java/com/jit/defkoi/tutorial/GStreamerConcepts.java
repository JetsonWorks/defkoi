package com.jit.defkoi.tutorial;

import org.freedesktop.gstreamer.*;

import java.util.concurrent.Semaphore;

// basic tutorial 2
public class GStreamerConcepts {
  private static Pipeline pipeline;
  private static Element source;
  private static Element sink;
  private static Semaphore gotEOSPipeline = new Semaphore(0);

  public static void main(String[] args) {
    Utils.configurePaths();
    Gst.init(Version.BASELINE, "BasicPipeline", args);
    try {
      source = ElementFactory.make("videotestsrc", "source");
      sink = ElementFactory.make("autovideosink", "sink");
      pipeline = new Pipeline();
    } catch(Exception e) {
      System.err.println(e);
      e.printStackTrace();
      System.exit(1);
    }

    try {
      pipeline.addMany(source, sink);
      pipeline.linkMany(source, sink);
      source.set("pattern", 0);
    } catch(Exception e) {
      System.err.println(e);
      e.printStackTrace();
      System.exit(1);
    }

    pipeline.getBus().connect((Bus.EOS)(source) -> {
      System.out.println("Received EOS on the pipeline");
      gotEOSPipeline.release();
    });
    pipeline.getBus().connect((Bus.ERROR)(source, code, message) -> {
      System.err.println("Received ERROR on the pipeline: " + message);
      gotEOSPipeline.release();
    });
    pipeline.play();

    try {
      Thread.sleep(100);
      State state = pipeline.getState();
      System.out.println("State: " + state);
    } catch(InterruptedException e) {
      System.err.println(e);
    }

    try {
      for(int i = 0; i < 30; i++) {
        Thread.sleep(500);
        source.set("pattern", i);
      }
    } catch(Exception e) {
      System.err.println(e);
      e.printStackTrace();
      System.exit(1);
    }

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
