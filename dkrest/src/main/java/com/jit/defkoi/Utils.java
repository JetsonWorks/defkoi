package com.jit.defkoi;

import com.jit.defkoi.jpa.Device;
import com.jit.defkoi.pipeline.CameraPipeline;
import com.sun.jna.Platform;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.device.DeviceProvider;
import org.freedesktop.gstreamer.device.DeviceProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Utils {
  private static final Logger logger = LoggerFactory.getLogger(CameraPipeline.class);

  private Utils() {
  }

  /**
   * Configures paths to the GStreamer libraries.
   */
  public static void configurePaths() {
    if(Platform.isLinux()) {
      String gstPath = System.getProperty("gstreamer.path", "/usr/lib/x86_64-linux-gnu/gstreamer-1.0");
      if(!gstPath.isEmpty()) {
        String jnaPath = System.getProperty("jna.library.path", "").trim();
        if(jnaPath.isEmpty()) {
          System.setProperty("jna.library.path", gstPath);
        } else {
          System.setProperty("jna.library.path", jnaPath + File.pathSeparator + gstPath);
        }
      }
    }
  }

  public static void printPadTemplates(Element elem) {
    StringBuilder sb = new StringBuilder();
    ElementFactory fac = elem.getFactory();
    List<StaticPadTemplate> pads = fac.getStaticPadTemplates();
    sb.append(String.format("\nPad templates for factory '%s':", fac.getLongName()));
    for(StaticPadTemplate pad : pads) {
      sb.append(String.format("\n %s template '%s'", pad.getDirection(), pad.getName()));
      sb.append(String.format("\n Availability: %s", pad.getPresence()));
      Caps caps = pad.getCaps();
      sb.append("\n " + caps);
    }
    logger.debug(sb.toString());
  }

  public static void formatCaps(Caps caps) {
    if(caps == null)
      return;
    for(int i = 0; i < caps.size(); i++)
      logger.debug(caps.getStructure(i).toString());
  }

  public static void dumpDevices() {
    List<DeviceProviderFactory> dpfs = DeviceProviderFactory.getDeviceProviders(PluginFeature.Rank.NONE);
    for(DeviceProviderFactory dpf : dpfs) {
      logger.debug("Found DeviceProviderFactory: " + dpf.getName());
      DeviceProvider dp = dpf.get();
      TreeSet<org.freedesktop.gstreamer.device.Device> devices =
        new TreeSet<>(Comparator.comparing(GstObject::getName));
      devices.addAll(dp.getDevices());
      for(org.freedesktop.gstreamer.device.Device d : devices) {
        logger.debug(String.format("Properties.for V4L2 device %s", d.getName()));
        for(String key : d.listPropertyNames()) {
          if(key.equals("caps"))
            continue;
          logger.debug(String.format("%s: %s", key, d.get(key)));
        }
        logger.debug("Caps for V4L2 device " + d.getName() + ": ");
        Utils.formatCaps(d.getCaps());
        logger.debug("");
      }
    }
  }

  public static List<Device> detectV4l2Devices() throws DefKoiException {
    List<Device> detected = new ArrayList<>();
    DeviceProvider dp = DeviceProviderFactory.getByName("v4l2deviceprovider");
    TreeSet<org.freedesktop.gstreamer.device.Device> devices = new TreeSet<>(Comparator.comparing(GstObject::getName));
    devices.addAll(dp.getDevices());
    for(org.freedesktop.gstreamer.device.Device d : devices)
      detected.add(new Device(d));
    return detected;
  }

  public static boolean detectNvidiaGst() {
    List<ElementFactory> efs = ElementFactory.listGetElements(ElementFactory.ListType.ANY, PluginFeature.Rank.NONE);
    if(efs.stream().anyMatch(x -> x.getName().equals("nvvideosink")))
      return true;
    return false;
  }

  public static List<String> captureNvargusCaps(int sensorId) throws IOException {
    ArrayList<String> output = new ArrayList<>();
    Process process = Runtime.getRuntime().exec("/usr/bin/gst-launch-1.0 nvarguscamerasrc sensor_id=" + sensorId);
    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    int grace = 9;
    new Timer().schedule(new TimerTask() {
      @Override
      public void run() {
        boolean success = false;
        try {
          success = process.waitFor(0, TimeUnit.SECONDS);
        } catch(InterruptedException e) {
        }
        if(!success) {
          logger.warn("After waiting " + grace
            + " seconds, the CLI pipeline to capture CSI caps did not terminate - might need to reboot");
          process.destroyForcibly();
        }
      }
    }, grace * 1000);
    output.addAll(readStdOut(reader));
    return output;
  }

  private static ArrayList<String> readStdOut(BufferedReader reader) {
    ArrayList<String> output = new ArrayList<>();
    String line;
    try {
      while((line = reader.readLine()) != null) {
        output.add(line);
        logger.debug(line);
      }
    } catch(IOException e) {
    }
    return output;
  }

}
