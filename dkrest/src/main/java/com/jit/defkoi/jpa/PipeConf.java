package com.jit.defkoi.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jit.defkoi.service.pref.Config;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Table(name = "pipe_conf")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class PipeConf extends BaseRestEntity {

  private static final Logger logger = LoggerFactory.getLogger(PipeConf.class);

  @ToString.Include
  private Boolean enabled;

  @ToString.Include
  @Column(name = "nv_enabled")
  private Boolean nvEnabled;

  @Transient
  private Integer deviceId;

  /** The selected device. */
  @JsonIgnore
  @ToString.Include
  @EqualsAndHashCode.Include
  @OneToOne
  @JoinColumn(name = "device_id")
  private Device device;

  @Transient
  private Integer capId;

  @ToString.Include
  @Column(name = "mot_detect_enabled")
  private Boolean motDetectEnabled;

  @ToString.Include
  @Column(name = "obj_detect_enabled")
  private Boolean objDetectEnabled;

  @Transient
  private Set<Capability> validCaps = new HashSet<>();

  /** The selected device capability. */
  @JsonIgnore
  @ToString.Include
  @EqualsAndHashCode.Include
  @OneToOne
  @JoinColumn(name = "cap_id")
  private Capability cap;

  public PipeConf(Device device) {
    this.device = device;
    device.setPipeConf(this);
    name = device.getDevicePath();
  }

  public PipeConf optimizeResolution(Config config) {
    Integer maxWidth = null;
    Integer maxHeight = null;
    if(config.isMotDetectEnabled() && Boolean.TRUE.equals(motDetectEnabled)) {
      maxWidth = config.getMaxMotDetectWidth();
      maxHeight = config.getMaxMotDetectHeight();
    }
    if(config.isObjDetectEnabled() && Boolean.TRUE.equals(objDetectEnabled)) {
      maxWidth = maxWidth == null ? config.getMaxObjDetectWidth() : Math.max(maxWidth, config.getMaxObjDetectWidth());
      maxHeight =
        maxHeight == null ? config.getMaxObjDetectHeight() : Math.max(maxHeight, config.getMaxObjDetectHeight());
    }
    int width = maxWidth == null ? config.getMaxCaptureWidth() : Math.min(maxWidth, config.getMaxCaptureWidth());
    int height = maxHeight == null ? config.getMaxCaptureHeight() : Math.min(maxHeight, config.getMaxCaptureHeight());

    // TODO: support for image/jpeg
    String capName = "video/x-raw";
    TreeSet<Capability> caps = new TreeSet<>(Comparator.comparingInt(o -> o.getWidth() * o.getHeight()));
    TreeSet<Capability> filteredCaps = new TreeSet<>(Comparator.comparingInt(o -> o.getWidth() * o.getHeight()));
    caps.addAll(device.getCaps().stream().filter(o -> capName.equals(o.getName())).collect(Collectors.toSet()));
    filteredCaps.addAll(
      device.getCaps().stream().filter(o -> capName.equals(o.getName())).filter(o -> o.getWidth().intValue() <= width)
        .filter(o -> o.getHeight().intValue() <= height).collect(Collectors.toSet()));
    try {
      cap = filteredCaps.last();
    } catch(NoSuchElementException e) {
      cap = caps.first();
    }
    return this;
  }

  public void setDevice(Device entity) {
    touch(entity);
    touch(device);
    device = entity;
  }

  public void setCap(Capability entity) {
    touch(entity);
    touch(cap);
    cap = entity;
  }

  @Override
  public void removeFromReferences() {
    super.removeFromReferences();
    if(device != null)
      device.setPipeConf(null);
    if(cap != null)
      cap.setPipeConf(null);
  }

}
