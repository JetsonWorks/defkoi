package com.jit.defkoi.service.pref;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jit.defkoi.config.DefaultConfig;
import com.jit.defkoi.jpa.pref.IPrefKey;
import com.jit.defkoi.jpa.pref.Preference;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;

import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Hashtable;

import static com.jit.defkoi.service.pref.ConfigPreferenceKey.*;

@NoArgsConstructor
public class Config extends CommonPreferences {

  private static final Logger logger = LoggerFactory.getLogger(Config.class);

  @JsonIgnore
  protected Hashtable<IPrefKey, Preference> prefs = new Hashtable<>();

  public Config(Collection<Preference> configPrefs) {
    for(Preference c : configPrefs)
      setPreference(c);
  }

  public Config(DefaultConfig defaultConfig, Config configPref)
    throws InvocationTargetException, IllegalAccessException {
    PropertyDescriptor[] pds = BeanUtils.getPropertyDescriptors(defaultConfig.getClass());
    for(PropertyDescriptor pd : pds) {
      try {
        Method getter = pd.getReadMethod();
        setPreference(ConfigPreferenceKey.valueOf(pd.getName()), (Serializable)getter.invoke(defaultConfig));
      } catch(IllegalArgumentException e) {
      }
    }

    pds = BeanUtils.getPropertyDescriptors(defaultConfig.getCameraConfig().getClass());
    for(PropertyDescriptor pd : pds) {
      try {
        Method getter = pd.getReadMethod();
        setPreference(ConfigPreferenceKey.valueOf(pd.getName()),
          (Serializable)getter.invoke(defaultConfig.getCameraConfig()));
      } catch(IllegalArgumentException e) {
      }
    }

    pds = BeanUtils.getPropertyDescriptors(defaultConfig.getDetectConfig().getClass());
    for(PropertyDescriptor pd : pds) {
      try {
        Method getter = pd.getReadMethod();
        setPreference(ConfigPreferenceKey.valueOf(pd.getName()),
          (Serializable)getter.invoke(defaultConfig.getDetectConfig()));
      } catch(IllegalArgumentException e) {
      }
    }

    patch(configPref);
  }

  @JsonIgnore
  public Collection<Preference> getPreferences() {
    return prefs.values();
  }

  public Preference getPreference(IPrefKey key) {
    Preference pref = prefs.get(key);
    if(pref == null) {
      pref = new Preference(key);
      prefs.put(key, pref);
    }
    return pref;
  }

  public Preference setPreference(IPrefKey key, Serializable value) {
    if(value == null) {
      prefs.remove(key);
      return null;
    }
    Preference pref = prefs.get(key);
    if(pref == null) {
      pref = new Preference(key);
      prefs.put(key, pref);
    }
    pref.value(value);
    return pref;
  }

  public void setPreference(Preference preference) {
    try {
      if(preference.getPrefKey() == null)
        preference.setPrefKey(ConfigPreferenceKey.valueOf(preference.getName()));
      prefs.put(preference.getPrefKey(), preference);
    } catch(IllegalArgumentException e) {
      logger.warn("Invalid name on saved preference: " + preference.getName());
    }
  }

  public boolean detectEquals(Config other) {
    return propsEqual(this.getEngine(), other.getEngine()) && propsEqual(this.getArtifactId(), other.getArtifactId())
      && propsEqual(this.getBackbone(), other.getBackbone()) && propsEqual(this.getFlavor(), other.getFlavor())
      && propsEqual(this.getDataset(), other.getDataset()) && propsEqual(this.getArgThreshold(),
      other.getArgThreshold());
  }

  public boolean propsEqual(Object s1, Object s2) {
    if(s1 != null) {
      if(s2 == null)
        return false;
      return s1.equals(s2);
    }
    return s2 == null;
  }

  // ignore common

  @Override
  @JsonIgnore
  public String getBurgerSide() {
    return super.getBurgerSide();
  }

  @Override
  @JsonIgnore
  public String getBurgerAnimation() {
    return super.getBurgerAnimation();
  }

  @Override
  @JsonIgnore
  public boolean getBurgerAutoHide() {
    return super.getBurgerAutoHide();
  }

  @Override
  @JsonIgnore
  public boolean getGridAnimatedRows() {
    return super.getGridAnimatedRows();
  }

  // General Config

  @JsonProperty("debug")
  public boolean isDebug() {
    return Boolean.TRUE.equals(getPreference(debug).getBooleanData());
  }

  @JsonProperty("debug")
  public void setDebug(boolean value) {
    setPreference(debug, value);
  }

  @JsonProperty("dockerized")
  public boolean isDockerized() {
    return Boolean.TRUE.equals(getPreference(dockerized).getBooleanData());
  }

  @JsonProperty("dockerized")
  public void setDockerized(boolean value) {
    setPreference(dockerized, value);
  }

  @JsonProperty("mediaDir")
  public String getMediaDir() {
    return getPreference(mediaDir).getStringData();
  }

  @JsonProperty("mediaDir")
  public void setMediaDir(String value) {
    setPreference(mediaDir, value);
  }

  @JsonProperty("maxImages")
  public Integer getMaxImages() {
    return getPreference(maxImages).getNumericData().intValue();
  }

  @JsonProperty("maxImages")
  public void setMaxImages(Integer value) {
    setPreference(maxImages, value);
  }

  @JsonProperty("maxVideos")
  public Integer getMaxVideos() {
    return getPreference(maxVideos).getNumericData().intValue();
  }

  @JsonProperty("maxVideos")
  public void setMaxVideos(Integer value) {
    setPreference(maxVideos, value);
  }

  @JsonProperty("videoLength")
  public Integer getVideoLength() {
    return getPreference(videoLength).getNumericData().intValue();
  }

  @JsonProperty("videoLength")
  public void setVideoLength(Integer value) {
    setPreference(videoLength, value);
  }

  @JsonProperty("labelFile")
  public String getLabelFile() {
    return getPreference(labelFile).getStringData();
  }

  @JsonProperty("labelFile")
  public void setLabelFile(String value) {
    setPreference(labelFile, value);
  }

  @JsonProperty("logStatsEnabled")
  public boolean isLogStatsEnabled() {
    return Boolean.TRUE.equals(getPreference(logStatsEnabled).getBooleanData());
  }

  @JsonProperty("logStatsEnabled")
  public void setLogStatsEnabled(boolean value) {
    setPreference(logStatsEnabled, value);
  }

  @JsonProperty("statsUpdatePeriod")
  public Integer getStatsUpdatePeriod() {
    return getPreference(statsUpdatePeriod).getNumericData().intValue();
  }

  @JsonProperty("statsUpdatePeriod")
  public void setStatsUpdatePeriod(Integer value) {
    setPreference(statsUpdatePeriod, value);
  }

  @JsonProperty("nvCapable")
  public boolean isNvCapable() {
    return Boolean.TRUE.equals(getPreference(nvCapable).getBooleanData());
  }

  @JsonProperty("nvCapable")
  public void setNvCapable(boolean value) {
    setPreference(nvCapable, value);
  }

  @JsonProperty("queueMaxSize")
  public Integer getQueueMaxSize() {
    return getPreference(queueMaxSize).getNumericData().intValue();
  }

  @JsonProperty("queueMaxSize")
  public void setQueueMaxSize(Integer value) {
    setPreference(queueMaxSize, value);
  }

  @JsonProperty("rtspProxyUrl")
  public void setRtspProxyUrl(String value) {
    setPreference(rtspProxyUrl, value);
  }

  @JsonProperty("rtspProxyUrl")
  public String getRtspProxyUrl() {
    return getPreference(rtspProxyUrl).getStringData();
  }

  @JsonProperty("tapLiveEnabled")
  public boolean isTapLiveEnabled() {
    return Boolean.TRUE.equals(getPreference(tapLiveEnabled).getBooleanData());
  }

  @JsonProperty("tapLiveEnabled")
  public void setTapLiveEnabled(boolean value) {
    setPreference(tapLiveEnabled, value);
  }

  @JsonProperty("liveRtspEnabled")
  public boolean isLiveRtspEnabled() {
    return Boolean.TRUE.equals(getPreference(liveRtspEnabled).getBooleanData());
  }

  @JsonProperty("liveRtspEnabled")
  public void setLiveRtspEnabled(boolean value) {
    setPreference(liveRtspEnabled, value);
  }

  @JsonProperty("objectRtspEnabled")
  public boolean isObjectRtspEnabled() {
    return Boolean.TRUE.equals(getPreference(objectRtspEnabled).getBooleanData());
  }

  @JsonProperty("objectRtspEnabled")
  public void setObjectRtspEnabled(boolean value) {
    setPreference(objectRtspEnabled, value);
  }

  // Default Camera Config

  @JsonProperty("maxCaptureWidth")
  public Integer getMaxCaptureWidth() {
    return getPreference(maxCaptureWidth).getNumericData().intValue();
  }

  @JsonProperty("maxCaptureWidth")
  public void setMaxCaptureWidth(Integer value) {
    setPreference(maxCaptureWidth, value);
  }

  @JsonProperty("maxCaptureHeight")
  public Integer getMaxCaptureHeight() {
    return getPreference(maxCaptureHeight).getNumericData().intValue();
  }

  @JsonProperty("maxCaptureHeight")
  public void setMaxCaptureHeight(Integer value) {
    setPreference(maxCaptureHeight, value);
  }

  @JsonProperty("maxCaptureRes")
  public String getMaxCaptureRes() {
    return String.format("%dx%d", getPreference(maxCaptureWidth).getNumericData(),
      getPreference(maxCaptureHeight).getNumericData());
  }

  @JsonProperty("maxCaptureRes")
  public void setMaxCaptureRes(String value) {
    String[] sizes = value.split("x");
    setPreference(maxCaptureWidth, Integer.parseInt(sizes[0]));
    setPreference(maxCaptureHeight, Integer.parseInt(sizes[1]));
  }

  // Detection Config

  @JsonProperty("objDetectEnabled")
  public boolean isObjDetectEnabled() {
    return Boolean.TRUE.equals(Boolean.TRUE.equals(getPreference(objDetectEnabled).getBooleanData()));
  }

  @JsonProperty("objDetectEnabled")
  public void setObjDetectEnabled(boolean value) {
    setPreference(objDetectEnabled, value);
  }

  @JsonProperty("maxObjDetectWidth")
  public Integer getMaxObjDetectWidth() {
    return getPreference(maxObjDetectWidth).getNumericData().intValue();
  }

  @JsonProperty("maxObjDetectWidth")
  public void setMaxObjDetectWidth(Integer value) {
    setPreference(maxObjDetectWidth, value);
  }

  @JsonProperty("maxObjDetectHeight")
  public Integer getMaxObjDetectHeight() {
    return getPreference(maxObjDetectHeight).getNumericData().intValue();
  }

  @JsonProperty("maxObjDetectHeight")
  public void setMaxObjDetectHeight(Integer value) {
    setPreference(maxObjDetectHeight, value);
  }

  @JsonProperty("maxObjDetectRes")
  public String getMaxObjDetectRes() {
    return String.format("%dx%d", getPreference(maxObjDetectWidth).getNumericData(),
      getPreference(maxObjDetectHeight).getNumericData());
  }

  @JsonProperty("maxObjDetectRes")
  public void setMaxObjDetectRes(String value) {
    String[] sizes = value.split("x");
    setPreference(maxObjDetectWidth, Integer.parseInt(sizes[0]));
    setPreference(maxObjDetectHeight, Integer.parseInt(sizes[1]));
  }

  @JsonProperty("motDetectEnabled")
  public boolean isMotDetectEnabled() {
    return Boolean.TRUE.equals(getPreference(motDetectEnabled).getBooleanData());
  }

  @JsonProperty("motDetectEnabled")
  public void setMotDetectEnabled(boolean value) {
    setPreference(motDetectEnabled, value);
  }

  @JsonProperty("motionGrayscale")
  public boolean isMotionGrayscale() {
    return Boolean.TRUE.equals(getPreference(motionGrayscale).getBooleanData());
  }

  @JsonProperty("motionGrayscale")
  public void setMotionGrayscale(boolean value) {
    setPreference(motionGrayscale, value);
  }

  @JsonProperty("maxMotDetectWidth")
  public Integer getMaxMotDetectWidth() {
    return getPreference(maxMotDetectWidth).getNumericData().intValue();
  }

  @JsonProperty("maxMotDetectWidth")
  public void setMaxMotDetectWidth(Integer value) {
    setPreference(maxMotDetectWidth, value);
  }

  @JsonProperty("maxMotDetectHeight")
  public Integer getMaxMotDetectHeight() {
    return getPreference(maxMotDetectHeight).getNumericData().intValue();
  }

  @JsonProperty("maxMotDetectHeight")
  public void setMaxMotDetectHeight(Integer value) {
    setPreference(maxMotDetectHeight, value);
  }

  @JsonProperty("maxMotDetectRes")
  public String getMaxMotDetectRes() {
    return String.format("%dx%d", getPreference(maxMotDetectWidth).getNumericData(),
      getPreference(maxMotDetectHeight).getNumericData());
  }

  @JsonProperty("maxMotDetectRes")
  public void setMaxMotDetectRes(String value) {
    String[] sizes = value.split("x");
    setPreference(maxMotDetectWidth, Integer.parseInt(sizes[0]));
    setPreference(maxMotDetectHeight, Integer.parseInt(sizes[1]));
  }

  @JsonProperty("objectSquelchPeriod")
  public Integer getObjectSquelchPeriod() {
    return getPreference(objectSquelchPeriod).getNumericData().intValue();
  }

  @JsonProperty("objectSquelchPeriod")
  public void setObjectSquelchPeriod(Integer value) {
    setPreference(objectSquelchPeriod, value);
  }

  @JsonProperty("engine")
  public String getEngine() {
    return getPreference(engine).getStringData();
  }

  @JsonProperty("engine")
  public void setEngine(String value) {
    setPreference(engine, value);
  }

  @JsonProperty("artifactId")
  public String getArtifactId() {
    return getPreference(artifactId).getStringData();
  }

  @JsonProperty("artifactId")
  public void setArtifactId(String value) {
    setPreference(artifactId, value);
  }

  @JsonProperty("backbone")
  public String getBackbone() {
    return getPreference(backbone).getStringData();
  }

  @JsonProperty("backbone")
  public void setBackbone(String value) {
    setPreference(backbone, value);
  }

  @JsonProperty("flavor")
  public String getFlavor() {
    return getPreference(flavor).getStringData();
  }

  @JsonProperty("flavor")
  public void setFlavor(String value) {
    setPreference(flavor, value);
  }

  @JsonProperty("dataset")
  public String getDataset() {
    return getPreference(dataset).getStringData();
  }

  @JsonProperty("dataset")
  public void setDataset(String value) {
    setPreference(dataset, value);
  }

  @JsonProperty("argThreshold")
  public Number getArgThreshold() {
    return getPreference(argThreshold).getNumericData();
  }

  @JsonProperty("argThreshold")
  public void setArgThreshold(Number value) {
    setPreference(argThreshold, value);
  }

  @JsonProperty("saveBoundingBoxImage")
  public boolean isSaveBoundingBoxImage() {
    return Boolean.TRUE.equals(getPreference(saveBoundingBoxImage).getBooleanData());
  }

  @JsonProperty("saveBoundingBoxImage")
  public void setSaveBoundingBoxImage(boolean value) {
    setPreference(saveBoundingBoxImage, value);
  }

}

