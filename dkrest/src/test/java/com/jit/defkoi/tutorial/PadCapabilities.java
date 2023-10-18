package com.jit.defkoi.tutorial;

import org.freedesktop.gstreamer.*;

import java.util.List;
import java.util.concurrent.Semaphore;

// basic tutorial 6
public class PadCapabilities {
  private Pipeline pipeline;
  private Element source;
  private Element sink;
  private Semaphore gotEOSPipeline = new Semaphore(0);

  public static void main(String[] args) {
    Utils.configurePaths();
    Gst.init(Version.BASELINE, "BasicPipeline", args);
    PadCapabilities app = new PadCapabilities();
    app.run();
  }

  PadCapabilities() {
    try {
      source = ElementFactory.make("audiotestsrc", "source");
      sink = ElementFactory.make("autoaudiosink", "sink");
      pipeline = new Pipeline("test-pipeline");
      printPadTemplates(source);
      printPadTemplates(sink);
    } catch(Exception e) {
      System.err.println("Not all elements could be created: " + e);
      e.printStackTrace();
      System.exit(1);
    }

    try {
      pipeline.addMany(source, sink);
      pipeline.linkMany(source, sink);
      System.out.println("Sink caps: " + formatCaps(sink.getStaticPad("sink").getCurrentCaps()));
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
      System.out.println("Sink caps: " + formatCaps(sink.getStaticPad("sink").getCurrentCaps()));
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

  private String formatCaps(Caps caps) {
    if(caps == null)
      return "null";
    StringBuilder sb = new StringBuilder();
    for(int i = 0; i < caps.size(); i++) {
      Structure struct = caps.getStructure(i);
      sb.append(String.format("Cap structure %s has %d fields", struct.getName(), struct.getFields()));
      sb.append(String.format("\n  Cap structure: %s", struct));
    }
    return sb.toString();
  }

  private void printPadTemplates(Element elem) {
    StringBuilder sb = new StringBuilder();
    ElementFactory fac = elem.getFactory();
    List<StaticPadTemplate> pads = fac.getStaticPadTemplates();
    sb.append(String.format("\nPad templates for factory '%s':", fac.getLongName()));
    for(StaticPadTemplate pad : pads) {
      sb.append(String.format("\n %s template '%s'", pad.getDirection(), pad.getName()));
      sb.append(String.format("\n Availability: %s", pad.getPresence()));
      Caps caps = pad.getCaps();
      sb.append("\n " + formatCaps(caps));
    }
    System.out.println(sb);
  }

}
