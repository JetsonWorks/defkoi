package com.jit.defkoi.tutorial;

import org.freedesktop.gstreamer.*;

import java.util.concurrent.Semaphore;

// basic tutorial 1
public class HelloWorld {
  private static Pipeline pipeline;
  private static Semaphore gotEOSPipeline = new Semaphore(1);

  public static void main(String[] args) {
    Utils.configurePaths();
    Gst.init(Version.BASELINE, "BasicPipeline", args);
    pipeline = (Pipeline)Gst.parseLaunch(
      "playbin uri=https://www.freedesktop.org/software/gstreamer-sdk/data/media/sintel_trailer-480p.webm", null);
    pipeline.play();
    pipeline.getBus().connect((Bus.EOS)(source) -> {
      System.out.println("Received EOS on the pipeline");
      gotEOSPipeline.release();
    });
    pipeline.getBus().connect((Bus.ERROR)(source, code, message) -> {
      System.err.println("Received ERROR on the pipeline");
      gotEOSPipeline.release();
    });

    try {
      Thread.sleep(100);
      State state = pipeline.getState();
      System.out.println("State: " + state);
    } catch(InterruptedException e) {
      System.err.println(e);
    }

    gotEOSPipeline.drainPermits();
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
