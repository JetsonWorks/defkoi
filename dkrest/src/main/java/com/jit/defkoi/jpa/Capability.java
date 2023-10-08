package com.jit.defkoi.jpa;

import com.jit.defkoi.DefKoiException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.freedesktop.gstreamer.Range;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.lowlevel.GValueAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Entity
@Table(name = "capability")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Capability extends BaseRestEntity {

  private final static Logger logger = LoggerFactory.getLogger(Capability.class);

  private static Field valField;
  private static Pattern rangePat = Pattern.compile("[0-9]+/[0-9]+");

  static {
    try {
      valField = Range.class.getDeclaredField("value");
    } catch(NoSuchFieldException e) {
      logger.error("Cannot reflect Range class: " + e, e);
    }
    valField.setAccessible(true);
  }

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "format", length = 50)
  private String format;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "width")
  private Integer width;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "height")
  private Integer height;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "aspect_ratio")
  private Double aspectRatio;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "framerate_min")
  private Double framerateMin;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "framerate_max")
  private Double framerateMax;

  @Transient
  private String deviceId;

  @ManyToOne
  @JoinColumn(name = "device_id")
  private Device device;

  @OneToOne
  @JoinColumn(name = "pipe_conf_id")
  private PipeConf pipeConf;

  public Capability(Structure cap) throws DefKoiException {
    name = cap.getName();
    if(cap.hasField("format"))
      this.format = cap.getString("format");
    if(cap.hasField("width"))
      this.width = cap.getInteger("width");
    if(cap.hasField("height"))
      height = cap.getInteger("height");
    if(cap.hasField("pixel-aspect-ratio"))
      aspectRatio = cap.getFraction("pixel-aspect-ratio").toDouble();
    if(cap.hasField("framerate"))
      try {
        framerateMin = cap.getFraction("framerate").toDouble();
        framerateMax = framerateMin;
      } catch(Structure.InvalidFieldException ife) {
        // seems to be a bug here - the Range min/max getters always return zero and the Range assertions fail:
        // gst_value_get_int_range_min: assertion 'GST_VALUE_HOLDS_INT_RANGE (value)' failed
        // gst_value_get_double_range_max: assertion 'GST_VALUE_HOLDS_DOUBLE_RANGE (value)' failed
        // gst_value_get_fraction_range_max: assertion 'GST_VALUE_HOLDS_FRACTION_RANGE (value)' failed
        try {
          // so we reflect the private "value", and parse the strdup
          GValueAPI.GValue val = (GValueAPI.GValue)valField.get(cap.getRange("framerate"));
          Matcher m = rangePat.matcher(val.toString());
          while(m.find()) {
            if(framerateMax == null)
              framerateMax = fractionToDouble(m.group());
            framerateMin = fractionToDouble(m.group());
          }
        } catch(IllegalAccessException e) {
          e.printStackTrace();
        }
      }

    // round framerates to nearest tenth
    if(framerateMin != null)
      framerateMin = BigDecimal.valueOf(framerateMin).setScale(1, RoundingMode.FLOOR).doubleValue();
    if(framerateMax != null)
      framerateMax = BigDecimal.valueOf(framerateMax).setScale(1, RoundingMode.FLOOR).doubleValue();
  }

  public Capability(Matcher m) {
    // GST_ARGUS: 3264 x 2464 FR = 21.000000 fps Duration = 47619048 ; Analog Gain range min 1.000000, max 10.625000; Exposure Range min 13000, max 683709000;
    name = "video/x-raw";
    format = "YUY2";
    width = Integer.parseInt(m.group(1));
    height = Integer.parseInt(m.group(2));
    aspectRatio = 1d;
    double framerate = Double.parseDouble(m.group(3));
    framerateMin = BigDecimal.valueOf(framerate).setScale(1, RoundingMode.FLOOR).doubleValue();
    framerateMax = framerateMin;
    /* TODO: any use for these?
    int duration = Integer.parseInt(m.group(4));
    double gainMin = Double.parseDouble(m.group(5));
    double gainMax = Double.parseDouble(m.group(6));
    int expRangeMin = Integer.parseInt(m.group(7));
    int expRangeMax = Integer.parseInt(m.group(8));
    */
  }

  public String getFormatted() {
    return String.format("%s %s %d %d %3.1f %3.1f %3.1f", name, format == null ? "\t" : format, width, height,
      aspectRatio, framerateMin, framerateMax);
  }

  public String prettyPrint() {
    String delim = "\n  ";
    return "Capability{" + delim + "name='" + name + '\'' + delim + "format='" + format + '\'' + delim + "width="
      + width + delim + "height=" + height + delim + "aspectRatio=" + aspectRatio + delim + "framerateMin="
      + framerateMin + delim + "framerateMax=" + framerateMax + '}';
  }

  protected Double fractionToDouble(String fraction) {
    String[] pcs = fraction.split("/");
    return Double.parseDouble(pcs[0]) / Integer.parseInt(pcs[1]);
  }

  public void setDevice(Device entity) {
    touch(entity);
    touch(device);
    device = entity;
  }

  public void setPipeConf(PipeConf entity) {
    touch(entity);
    touch(pipeConf);
    pipeConf = entity;
  }

  @Override
  public void removeFromReferences() {
    super.removeFromReferences();
    if(pipeConf != null)
      pipeConf.setCap(null);
    if(device != null)
      device.getCaps().remove(this);
    touch(device);
  }

}
