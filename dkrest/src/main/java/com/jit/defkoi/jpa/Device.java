package com.jit.defkoi.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.jit.defkoi.DefKoiException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.beanutils.BeanUtils;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import javax.persistence.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Entity
@Table(name = "device")
@PrimaryKeyJoinColumn(name = "id")
@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(callSuper = true, onlyExplicitlyIncluded = true)
@NoArgsConstructor
public class Device extends BaseRestEntity {

  private static Hashtable<String, String> deviceMap = new Hashtable<>();
  private static Hashtable<String, String> devicePropMap = new Hashtable<>();
  private static Pattern nvargusPat = Pattern.compile(
    "GST_ARGUS: ([0-9]+) x ([0-9]+) FR = ([0-9\\.]+) fps Duration = ([0-9]+) ;"
      + " Analog Gain range min ([0-9\\.]+), max ([0-9\\.]+); Exposure Range min ([0-9]+), max ([0-9]+);");
  private static Pattern cameraModePat = Pattern.compile(" *Camera mode  = ([0-9]+).*");

  static {
    deviceMap.put("name", "name");
    deviceMap.put("display-name", "displayName");
    deviceMap.put("device-class", "deviceClass");
    deviceMap.put("device-path", "devicePath");

    devicePropMap.put("device.api", "deviceApi");
    devicePropMap.put("v4l2.device.driver", "deviceDriver");
    devicePropMap.put("v4l2.device.card", "deviceCard");
    devicePropMap.put("v4l2.device.version", "deviceVersion");
    devicePropMap.put("v4l2.device.bus_info", "deviceBusInfo");
  }

  private Boolean enabled;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "display_name", length = 50)
  private String displayName;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "device_class", length = 50)
  private String deviceClass;

  @ToString.Include
  @Column(name = "device_path", length = 50)
  private String devicePath;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "device_api", length = 50)
  private String deviceApi;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "device_driver", length = 50)
  private String deviceDriver;

  @ToString.Include
  @Column(name = "device_version", length = 50)
  private Integer deviceVersion;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "device_card", length = 50)
  private String deviceCard;

  @EqualsAndHashCode.Include
  @ToString.Include
  @Column(name = "device_bus_info", length = 50)
  private String deviceBusInfo;

  @ToString.Include
  @Column(name = "camera_mode")
  private Integer cameraMode;

  @ToString.Include
  @JsonIgnore
  @OneToMany(mappedBy = "device", cascade = CascadeType.ALL)
  @Fetch(FetchMode.JOIN)
  private Set<Capability> caps = new HashSet<>();

  @JsonIgnore
  @OneToOne(cascade = CascadeType.ALL)
  @JoinColumn(name = "pipe_conf_id")
  private PipeConf pipeConf;

  public Device(org.freedesktop.gstreamer.device.Device device) throws DefKoiException {
    for(String key : deviceMap.keySet()) {
      try {
        BeanUtils.setProperty(this, deviceMap.get(key), device.getAsString(key));
      } catch(IllegalAccessException | InvocationTargetException e) {
        throw new DefKoiException("Could not set all device properties: " + e, e);
      }
    }
    for(String key : devicePropMap.keySet()) {
      try {
        if(device.getProperties().hasField(key)) {
          if(key == "v4l2.device.version")
            BeanUtils.setProperty(this, devicePropMap.get(key), (Integer)device.getProperties().getValue(key));
          else
            BeanUtils.setProperty(this, devicePropMap.get(key), device.getProperties().getString(key));
        }
      } catch(IllegalAccessException | InvocationTargetException e) {
        throw new DefKoiException("Could not set all device properties: " + e, e);
      }
    }
    for(int i = 0; i < device.getCaps().size(); i++)
      caps.add(new Capability(device.getCaps().getStructure(i)));
    caps.forEach(x -> x.setDevice(this));
  }

  public Device(String devicePath, List<String> output) throws DefKoiException {
    this.devicePath = devicePath;
    for(String line : output) {
      Matcher m = nvargusPat.matcher(line);
      if(m.matches()) {
        Capability c = new Capability(m);
        c.setDevice(this);
        caps.add(c);
      } else {
        m = cameraModePat.matcher(line);
        if(m.matches()) {
          this.cameraMode = Integer.parseInt(m.group(1));
        }
      }
    }
  }

  @JsonIgnore
  public Capability getMaxRes() throws DefKoiException {
    return caps.stream().sorted(Comparator.comparingInt(o -> o.getWidth() * o.getHeight())).findFirst()
      .orElseThrow(() -> DefKoiException.deviceNoCaps(name));
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName.replaceAll("[\\\\\"]", "").replaceAll(":", "-");
  }

  public String prettyPrint() {
    String delim = "\n";
    return "Device{" + delim + "name='" + name + '\'' + delim + "displayName='" + displayName + '\'' + delim
      + "deviceClass='" + deviceClass + '\'' + delim + "devicePath='" + devicePath + '\'' + delim + "deviceApi='"
      + deviceApi + '\'' + delim + "deviceDriver='" + deviceDriver + '\'' + delim + "deviceCard='" + deviceCard + '\''
      + delim + "deviceVersion='" + deviceVersion + '\'' + delim + "caps=" + caps.stream().map(Capability::toString)
      .collect(Collectors.joining(",\n")) + '}';
  }

  @Override
  public void setRetireTime(Date retireTime) {
    super.setRetireTime(retireTime);
    caps.forEach(x -> {
      x.setRetireTime(retireTime);
    });
  }

  public void setPipeConf(PipeConf entity) {
    touch(entity);
    touch(pipeConf);
    pipeConf = entity;
  }

  @Override
  public void removeFromReferences() {
    super.removeFromReferences();
    if(pipeConf != null) {
      pipeConf.setDevice(null);
      pipeConf.setCap(null);
    }
  }

}
