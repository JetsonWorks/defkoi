package com.jit.defkoi.pipeline;

import ai.djl.modality.cv.Image;
import org.freedesktop.gstreamer.Buffer;
import org.freedesktop.gstreamer.Sample;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ImageUtils {

  private static final Logger logger = LoggerFactory.getLogger(ImageUtils.class);

  public static BufferedImage cvImageToBufferedImage(Image image) {
    int size = image.getWidth() * image.getHeight();
    IntBuffer intBuffer =
      IntBuffer.wrap(((DataBufferInt)((BufferedImage)image.getWrappedImage()).getRaster().getDataBuffer()).getData());

    BufferedImage annotated = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
    int[] destPixels = ((DataBufferInt)annotated.getRaster().getDataBuffer()).getData();
    intBuffer.get(destPixels, 0, size);
    return annotated;
  }

  // Internal data stream error when trying to pipe image data to rtspclientsink, even via videoconvert and queue
  public static Buffer cvImageToBuffer(Image image) {
    return bufferedImageToBuffer((BufferedImage)image.getWrappedImage());
  }

  public static Buffer bufferedImageToBuffer(BufferedImage image) {
    DataBufferInt dbi = (DataBufferInt)image.getRaster().getDataBuffer();
    IntBuffer intBuffer = IntBuffer.wrap(dbi.getData());
    Buffer buffer = new Buffer(image.getWidth() * image.getHeight() * 4);
    ByteBuffer bbuf = buffer.map(true);
    bbuf.asIntBuffer().put(intBuffer);
    buffer.unmap();
    return buffer;
  }

  // note: Image.save() uses a file cache when needed
  public static Buffer cvImageToBufferPng(Image image) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    image.save(baos, "png");
    Buffer buffer = new Buffer(baos.size());
    ByteBuffer bbuf = buffer.map(true);
    bbuf.put(0, baos.toByteArray(), 0, baos.size());
    buffer.unmap();
    return buffer;
  }

  public static BufferedImage sampleToBufferedImage(Sample sample) throws IOException {
    ByteBuffer bb = sample.getBuffer().map(false);
    logger.trace("capacity: " + bb.capacity());
    byte[] dest = new byte[bb.capacity()];
    bb.get(dest, 0, dest.length);
    ByteArrayInputStream bais = new ByteArrayInputStream(dest);
    return ImageIO.read(bais);
  }

}
