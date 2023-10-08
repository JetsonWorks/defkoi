package com.jit.defkoi.pipeline;

import com.jit.defkoi.DefKoiException;
import com.jit.defkoi.Utils;
import com.jit.defkoi.service.DeviceApi;
import com.jit.defkoi.service.DeviceService;
import lombok.Getter;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.AppSrc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Hashtable;
import java.util.Map;

public class Pipe {
  private static final Logger logger = LoggerFactory.getLogger(Pipe.class);

  private Pipeline pipeline;

  @Getter
  private DeviceApi deviceApi;

  @Getter
  private String type;
  @Getter
  private String name;
  private AppSink.NEW_SAMPLE newSampleListener;
  private AppSrc.NEED_DATA needDataListener;
  private AppSrc.ENOUGH_DATA enoughDataListener;
  private String cap = "";

  private Element first;
  private Element uriDecodeBinTarget;
  private Element last;
  private Element target;
  private boolean nv;
  private boolean converter;

  private ImageFormat format;
  private boolean formatted;
  private String aspectRatio;
  private boolean aspected;
  private Integer width;
  private Integer height;
  private boolean scaled;
  private Hashtable<String, Object> properties = new Hashtable<>();

  private Pipe() {
  }

  public static Pipe source(Pipeline pipeline, DeviceApi deviceApi, String name) {
    Pipe pipe = new Pipe();
    pipe.converter = true;
    pipe.pipeline = pipeline;
    pipe.deviceApi = deviceApi;
    pipe.name = name;
    return pipe;
  }

  public static Pipe convert(Pipeline pipeline, String name) {
    Pipe pipe = new Pipe();
    pipe.converter = true;
    pipe.pipeline = pipeline;
    pipe.name = name;
    return pipe;
  }

  public static Pipe appSink(Pipeline pipeline, String name, AppSink.NEW_SAMPLE newSampleListener) {
    Pipe pipe = new Pipe();
    pipe.pipeline = pipeline;
    pipe.type = "appsink";
    pipe.name = name;
    pipe.newSampleListener = newSampleListener;
    pipe.set("emit-signals", true);
    return pipe;
  }

  public static Pipe appSource(Pipeline pipeline, String name) {
    Pipe pipe = new Pipe();
    pipe.pipeline = pipeline;
    pipe.type = "appsrc";
    pipe.name = name;
    pipe.set("emit-signals", true);
    return pipe;
  }

  public static Pipe lay(Pipeline pipeline, String type, String name) {
    Pipe pipe = new Pipe();
    pipe.pipeline = pipeline;
    pipe.type = type;
    pipe.name = name;
    return pipe;
  }

  public Pipe cap(String cap) {
    this.cap = cap;
    return this;
  }

  public Pipe width(Integer width) {
    this.width = width;
    scaled = true;
    return this;
  }

  public Pipe height(Integer height) {
    this.height = height;
    scaled = true;
    return this;
  }

  public Pipe format(ImageFormat format) {
    this.format = format;
    formatted = true;
    return this;
  }

  public Pipe aspect(String ratio) {
    this.aspectRatio = ratio;
    aspected = true;
    return this;
  }

  public Pipe nv(boolean isNv) {
    this.nv = isNv;
    return this;
  }

  public Pipe set(String key, Object value) {
    if(value != null)
      properties.put(key, value);
    return this;
  }

  public Pipe needDataListener(AppSrc.NEED_DATA listener) {
    this.needDataListener = listener;
    return this;
  }

  public Pipe enoughDataListener(AppSrc.ENOUGH_DATA listener) {
    this.enoughDataListener = listener;
    return this;
  }

  /** Returns the Pipe link target. */
  public Pipe link(Pipe target) throws DefKoiException {
    link(target.getFirst());
    return target;
  }

  public void link(Element target) throws DefKoiException {
    if("appsink".equals(type))
      throw new UnsupportedOperationException("Elements of type appsink should be built, not linked");
    if(first == null)
      build();

    if("uridecodebin".equals(type)) {
      if(uriDecodeBinTarget == null) {
        this.target = target;
        first.connect((Element.PAD_ADDED)this::handlePadAdded);
      } else {
        this.target = uriDecodeBinTarget;
        first.connect((Element.PAD_ADDED)this::handlePadAdded);
        pipeline.linkMany(uriDecodeBinTarget, last);
      }
    }
    logger.trace(String.format("Linking %s to %s", last.getName(), target.getName()));
    if(!pipeline.linkMany(last, target))
      throw new DefKoiException("Failed to link " + last.getName() + " to " + target.getName());
  }

  /**
   * Builds this section of the pipeline. Formatting and scaling can be applied if this Pipe was constructed using
   * Pipe.source() or Pipe.convert(). Call Pipe.nv(true) to take advantage of GPU processing provided by NVidia GST
   * plugins.
   * @return this Pipe object
   */
  public Pipe build() {
    if(first != null)
      throw new IllegalStateException("This Pipe has already been built.");
    if(nv) {
      try {
        doBuildNv();
        return this;
      } catch(IllegalArgumentException e) {
        if(type != null && type.startsWith("nv"))
          logger.warn("Element '" + type + "' was not found. Using standard element(s) instead.");
        else
          throw e;
      }
    }
    doBuild();
    return this;
  }

  private void doBuildNv() throws IllegalArgumentException {
    logger.trace("NV-building " + name);
    type = DeviceService.getEfNameByApiAccel(deviceApi, nv);
    if(type == null)
      type = "nvvidconv";
    logger.trace(String.format("Making %s of type %s", name, type));
    Element element = ElementFactory.make(type, name);
    first = element;
    last = element;
    uriDecodeBinTarget = element;

    setProperties(element);
    if(newSampleListener != null)
      ((AppSink)element).connect(newSampleListener);
    if(needDataListener != null)
      ((AppSrc)element).connect(needDataListener);
    if(enoughDataListener != null)
      ((AppSrc)element).connect(enoughDataListener);
    pipeline.add(element);

    if(formatted || aspected || scaled) {
      if(!converter)
        throw new IllegalArgumentException(
          "Please don't specify conversion or scaling on Pipe elements of type " + type);

      logger.trace(String.format("%s requires capsfilter (%s)", name, name + "Filter"));
      StringBuilder caps = new StringBuilder("video/x-raw(memory:NVMM)");
      if(formatted)
        caps.append(String.format("%sformat=%s", caps.length() > 0 ? "," : "", format.getGstFormatString()));
      if(aspected)
        caps.append(String.format("%spixel-aspect-ratio=%s", caps.length() > 0 ? "," : "", aspectRatio));
      if(scaled) {
        if(width != null)
          caps.append(String.format("%swidth=%d", caps.length() > 0 ? "," : "", width));
        if(height != null)
          caps.append(String.format("%sheight=%d", caps.length() > 0 ? "," : "", height));
      }
      uriDecodeBinTarget = ElementFactory.make("capsfilter", name + "Filter");
      uriDecodeBinTarget.setCaps(new Caps(caps.toString()));
      pipeline.add(uriDecodeBinTarget);
      last = uriDecodeBinTarget;

      // sources of type 'uridecodebin' get linked to filter later
      if(!"uridecodebin".equals(type)) {
        logger.trace(String.format("Linking %s to %s", first.getName(), uriDecodeBinTarget.getName()));
        pipeline.linkMany(first, uriDecodeBinTarget);
      }
    }

  }

  private void doBuild() {
    logger.trace("Building " + name);
    Element formatFilter = null;
    Element scaleFilter = null;
    boolean isFormatting = StringUtils.hasLength(cap) || formatted;
    boolean isScaling = aspected || scaled;
    // default element, in case no formatting or scaling
    Element element = null;
    if(deviceApi != null)
      type = DeviceService.getEfNameByApiAccel(deviceApi, nv);
    if(type == null) {
      logger.trace(String.format("Making default %s of type %s", name, "videoconvert"));
      element = ElementFactory.make("videoconvert", name);
    }
    first = element;
    last = element;
    uriDecodeBinTarget = element;

    if((isFormatting || isScaling) && !converter)
      throw new IllegalArgumentException("Please don't specify conversion or scaling on Pipe elements of type " + type);
    if(type != null) {
      logger.trace(String.format("Making %s of type %s", name, type));
      element = ElementFactory.make(type, name);
      first = element;
      last = element;
      uriDecodeBinTarget = element;
      try {
        setProperties(element);
      } catch(IllegalArgumentException e) {
        throw new IllegalArgumentException("Could not set properties on element " + name, e);
      }
      if(newSampleListener != null)
        ((AppSink)element).connect(newSampleListener);
      if(needDataListener != null)
        ((AppSrc)element).connect(needDataListener);
      if(enoughDataListener != null)
        ((AppSrc)element).connect(enoughDataListener);
    }
    pipeline.add(element);

    if(isFormatting) {
      logger.trace(String.format("%s requires capsfilter (%s)", name, name + "FormatFilter"));
      StringBuilder caps = new StringBuilder(StringUtils.hasLength(cap) ? cap : "");
      if(formatted)
        caps.append(String.format("%sformat=%s", caps.length() > 0 ? "," : "", format.getGstFormatString()));
      formatFilter = ElementFactory.make("capsfilter", name + "FormatFilter");
      formatFilter.setCaps(new Caps(caps.toString()));
      pipeline.add(formatFilter);
    }

    if(isScaling) {
      logger.trace(String.format("%s requires capsfilter (%s)", name, name + "ScaleFilter"));
      StringBuilder caps = new StringBuilder(StringUtils.hasLength(cap) ? cap : "");
      if(aspected)
        caps.append(String.format("%spixel-aspect-ratio=%s", caps.length() > 0 ? "," : "", aspectRatio));
      if(scaled) {
        if(width != null)
          caps.append(String.format("%swidth=%d", caps.length() > 0 ? "," : "", width));
        if(height != null)
          caps.append(String.format("%sheight=%d", caps.length() > 0 ? "," : "", height));
      }
      scaleFilter = ElementFactory.make("capsfilter", name + "ScaleFilter");
      scaleFilter.setCaps(new Caps(caps.toString()));
      pipeline.add(scaleFilter);
    }

    if(isFormatting || isScaling) {
      if(isFormatting) {
        logger.trace(String.format("Making %s of type %s", name + "Formatter", "videoconvert"));
        Element formatter = ElementFactory.make("videoconvert", name + "Formatter");
        if(first == null)
          first = formatter;
        uriDecodeBinTarget = formatter;
        pipeline.add(formatter);
        logger.trace(String.format("Linking %s to %s", formatter.getName(), formatFilter.getName()));
        pipeline.linkMany(formatter, formatFilter);
        last = formatFilter;
        if(isScaling) {
          logger.trace(String.format("Making %s of type %s", name + "Scaler", "videoscale"));
          Element scaler = ElementFactory.make("videoscale", name + "Scaler");
          pipeline.add(scaler);
          logger.trace(
            String.format("Linking %s to %s to %s", formatFilter.getName(), scaler.getName(), scaleFilter.getName()));
          pipeline.linkMany(formatFilter, scaler, scaleFilter);
          last = scaleFilter;
        }
      } else if(isScaling) {
        logger.trace(String.format("Making %s of type %s", name + "Scaler", "videoscale"));
        Element scaler = ElementFactory.make("videoscale", name + "Scaler");
        if(first == null)
          first = scaler;
        uriDecodeBinTarget = scaler;
        pipeline.add(scaler);
        logger.trace(String.format("Linking %s to %s", scaler.getName(), scaleFilter.getName()));
        pipeline.linkMany(scaler, scaleFilter);
        last = scaleFilter;
      }
      // sources of type 'uridecodebin' get linked to filter later
      if(!"uridecodebin".equals(type)) {
        logger.trace(String.format("Linking %s to %s", first.getName(), uriDecodeBinTarget.getName()));
        pipeline.linkMany(first, uriDecodeBinTarget);
      }
    }
  }

  private void setProperties(Element element) {
    for(Map.Entry<String, Object> prop : properties.entrySet()) {
      logger.trace(String.format("Setting %s property to %s", element.getName(), prop.getKey()), "" + prop.getValue());
      if(prop.getKey().equals("uri")) {
        if("uridecodebin".equals(type))
          element.set(prop.getKey(), prop.getValue());
      } else if(prop.getKey().equals("device")) {
        if("v4l2src".equals(type))
          element.set(prop.getKey(), prop.getValue());
      } else
        element.set(prop.getKey(), prop.getValue());
    }
  }

  /** Link from source to target. */
  private void handlePadAdded(Element source, Pad pad) {
    logger.debug(String.format("Received new pad '%s' from '%s'", pad.getName(), source.getName()));
    if(((Element)source).getSrcPads().size() > 0)
      Utils.formatCaps(((Element)source).getSrcPads().get(0).getCurrentCaps());
    if(pad.isLinked()) {
      logger.debug("New pad '" + pad.getName() + "' is already linked");
      return;
    }

    String padCapTypeName = pad.getCurrentCaps().getStructure(0).getName();
    logger.debug(String.format("New pad '%s' caps: %s", pad.getName(), pad.getCurrentCaps()));
    if(!padCapTypeName.startsWith("video/x-raw")) {
      logger.debug(String.format("New pad '%s' cap has type %s; ignoring", pad.getName(), padCapTypeName));
      return;
    }

    try {
      pad.link(target.getStaticPad("sink"));
      logger.debug(String.format("New pad '%s' successfully linked to %s (type %s)", pad.getName(), target.getName(),
        padCapTypeName));
    } catch(PadLinkException e) {
      logger.error(String.format("%s's pad '%s' cap type is %s but link failed: %s", target.getName(), pad.getName(),
        padCapTypeName, e));
      throw e;
    }
  }

  public Element getFirst() {
    if(first == null)
      build();
    return first;
  }

}
