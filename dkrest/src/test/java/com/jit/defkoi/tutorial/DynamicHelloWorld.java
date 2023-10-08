package com.jit.defkoi.tutorial;

import org.freedesktop.gstreamer.*;

import java.util.concurrent.Semaphore;

// basic tutorial 3
public class DynamicHelloWorld {
  private Pipeline pipeline;
  private Element source;
  private Element convert;
  private Element resample;
  private Element sink;
  private Semaphore gotEOSPipeline = new Semaphore(0);

  public static void main(String[] args) {
    Utils.configurePaths();
    Gst.init(Version.BASELINE, "BasicPipeline", args);
    DynamicHelloWorld app = new DynamicHelloWorld();
    app.run();
  }

  DynamicHelloWorld() {
    try {
      source = ElementFactory.make("uridecodebin", "source");
      convert = ElementFactory.make("audioconvert", "convert");
      resample = ElementFactory.make("audioresample", "resample");
      sink = ElementFactory.make("autoaudiosink", "sink");
      pipeline = new Pipeline("test-pipeline");
    } catch(Exception e) {
      System.err.println("Not all elements could be created: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    try {
      pipeline.addMany(source, convert, resample, sink);
      // do not link source with convert because source contains no pads
      pipeline.linkMany(convert, resample, sink);
    } catch(Exception e) {
      System.err.println("Elements could not be linked: " + e);
      e.printStackTrace();
      System.exit(1);
    }
  }

  public void run() {
    source.set("uri", "https://www.freedesktop.org/software/gstreamer-sdk/data/media/sintel_trailer-480p.webm");
    source.connect((Element.PAD_ADDED)this::handlePadAdded);

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

  private void handlePadAdded(Element elem, Pad pad) {
    System.out.println(String.format("Received new pad '%s' from '%s'", pad.getName(), elem.getName()));
    if(pad.isLinked()) {
      System.out.println("Pad '" + pad.getName() + "' is already linked");
      return;
    }

    String padCapTypeName = pad.getCurrentCaps().getStructure(0).getName();
    if(!padCapTypeName.startsWith("audio/x-raw")) {
      System.out.println(String.format("Pad '%s' cap has type %s; ignoring", pad.getName(), padCapTypeName));
      return;
    }
    System.out.println(String.format("Pad '%s' caps: %s", pad.getName(), pad.getCurrentCaps()));

    try {
      Pad csink = convert.getStaticPad("sink");
      System.out.println("Convert's sink can accept new pad's caps: " + csink.queryAcceptCaps(pad.getCurrentCaps()));
      pad.link(convert.getStaticPad("sink"));
      System.out.println(String.format("Pad '%s' link succeeded (type %s)", pad.getName(), padCapTypeName));
    } catch(PadLinkException e) {
      System.err.println(
        String.format("Pad '%s' cap type is %s but link failed: %s", pad.getName(), padCapTypeName, e));
    }
  }

}
