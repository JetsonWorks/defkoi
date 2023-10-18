package com.jit.defkoi.pipeline;

import com.jit.defkoi.SentryRuntime;
import com.jit.defkoi.ThreadGroupLocal;
import lombok.Getter;
import org.freedesktop.gstreamer.FlowReturn;
import org.freedesktop.gstreamer.Sample;
import org.freedesktop.gstreamer.elements.AppSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;

import static com.jit.defkoi.SentryRuntime.Directive.reinitialize;

/**
 * Just pulls images from incoming Samples, converts them, and places them on the motionConverted queue. Runs in the
 * CameraPipeline's thread.
 */
public class MotionConversion implements AppSink.NEW_SAMPLE {
  private static final Logger logger = LoggerFactory.getLogger(MotionConversion.class);

  @Getter
  private SentryRuntime runtime = new SentryRuntime(logger);
  protected PipelineContext context;
  protected Integer imageWidth, imageHeight, numPixels;
  protected String format;
  private Integer yuvSize;
  private byte[] yuv; // reuse to reduce garbage

  protected BufferedImage previous;
  protected BufferedImage image;
  protected BoundedQueue<Integer> positiveResults = new BoundedQueue<>(10);
  protected double successThreshold = 0.5;
  protected int minSamples = 5;

  public MotionConversion() {
    this.context = new ThreadGroupLocal<PipelineContext>().getValue();
    runtime.setName(context.getCameraPipeline().getName() + "." + this.getClass().getSimpleName());
    runtime.run();
    runtime.running();
  }

  public FlowReturn newSample(AppSink appSink) {
    logger.trace(MotionConversion.class.getSimpleName() + " received new sample");
    Metric cost = context.getPipelineStats().getMotConversion();
    if(!context.getConfig().isMotDetectEnabled() || !context.getPipeConf().getMotDetectEnabled()) {
      Sample sample = appSink.pullSample();
      sample.dispose();
      positiveResults.add(1);
      return FlowReturn.OK;
    }

    BufferedImage image = pullSampleBufferImage(appSink, cost);
    if(image == null) {
      positiveResults.add(0);
      if(getSuccessRate() < successThreshold && positiveResults.size() >= minSamples) {
        logger.info(String.format("Success rate (%f) < threshold (%f)", getSuccessRate(), successThreshold));
        context.killCameraPipeline();
        return FlowReturn.ERROR;
      }
      logger.debug(String.format("Success rate (%f) >= threshold (%f)", getSuccessRate(), successThreshold));
      return FlowReturn.OK;
    }
    context.getMotionConverted().add(image);
    positiveResults.add(1);
    return FlowReturn.OK;
  }

  protected BufferedImage pullSampleBufferImage(AppSink appSink, Metric cost) {
    cost.startClock();
    Sample sample = appSink.pullSample();
    if(numPixels == null || runtime.equals(reinitialize)) {
      runtime.reinitializing();
      logger.debug(MotionConversion.class.getSimpleName() + " sampled caps: " + sample.getCaps());
      imageWidth = sample.getCaps().getStructure(0).getInteger("width");
      imageHeight = sample.getCaps().getStructure(0).getInteger("height");
      numPixels = imageWidth * imageHeight;
      format = sample.getCaps().getStructure(0).getString("format");
      if(format != null && format.indexOf("GRAY") > -1)
        numPixels /= 4;
      // YUV
      double pengali = 1.5;
      yuvSize = (int)(numPixels * pengali);
      yuv = new byte[yuvSize];
      runtime.run();
      runtime.running();
    }

    try {
      ByteBuffer bb = sample.getBuffer().map(false);
      if(sample.getCaps().getStructure(0).getName().equals("image/jpeg"))
        image = ImageUtils.sampleToBufferedImage(sample);
      else
        image = format.equals(ImageFormat.i420.getGstFormatString()) ? rgbFromI420Buffer(bb) : rgbFromBgrx(bb);
      sample.getBuffer().unmap();
    } catch(BufferUnderflowException e) {
      logger.error(e.toString(), e);
      return null;
    } catch(Exception e) {
      logger.error(e.toString(), e);
    } finally {
      previous = image;
      sample.dispose();
      cost.stopClock();
    }
    return image;
  }

  public void reinit() {
    runtime.reinitialize();
  }

  protected BufferedImage rgbFromBgrx(ByteBuffer sampleByteBuffer) {
    logger.trace("capacity: " + sampleByteBuffer.capacity());
    BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
    int[] destPixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
    sampleByteBuffer.asIntBuffer().get(destPixels, 0, numPixels);
    return image;
  }

  protected BufferedImage rgbFromI420Buffer(ByteBuffer sampleByteBuffer) {
    logger.trace("input capacity: " + sampleByteBuffer.capacity());
    sampleByteBuffer.get(yuv, 0, yuvSize);
    BufferedImage rgb = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);
    for(int j = 0; j < imageHeight; j++) {
      for(int i = 0; i < imageWidth; i++) {
        int rColor = getRGBFromStream(yuv, i, j);
        rgb.setRGB(i, j, rColor);
      }
    }
    return rgb;
  }

  protected int getRGBFromStream(byte[] oneFrame, int x, int y) {
    int Y = unsignedByteToInt(oneFrame[y * imageWidth + x]);
    int U = unsignedByteToInt(oneFrame[(y / 2) * (imageWidth / 2) + x / 2 + numPixels]);
    int V = unsignedByteToInt(oneFrame[(y / 2) * (imageWidth / 2) + x / 2 + numPixels + numPixels / 4]);

    //~ int R = (int)(Y + 1.370705 * (V-128));
    //~ int G = (int)(Y - 0.698001 * (V-128) - 0.337633 * (U-128));
    //~ int B = (int)(Y + 1.732446 * (U-128));

    int r = (int)(Y + 1.4075 * (V - 128));
    int g = (int)(Y - 0.3455 * (U - 128) - (0.7169 * (V - 128)));
    int b = (int)(Y + 1.7790 * (U - 128));

    r = Math.min(Math.max(r, 0), 255);
    g = Math.min(Math.max(g, 0), 255);
    b = Math.min(Math.max(b, 0), 255);
    int rColor = (0xff << 24) | (r << 16) | (g << 8) | b;
    return rColor;
  }

  protected static int unsignedByteToInt(byte b) {
    return (int)b & 0xFF;
  }

  protected double getSuccessRate() {
    double total = 0.0;
    for(Integer result : positiveResults)
      total += result;
    return BigDecimal.valueOf(total / positiveResults.size()).setScale(2, RoundingMode.HALF_UP).doubleValue();
  }

}

