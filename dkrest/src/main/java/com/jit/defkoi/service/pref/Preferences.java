package com.jit.defkoi.service.pref;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jit.defkoi.jpa.User;
import com.jit.defkoi.jpa.pref.Preference;
import lombok.NoArgsConstructor;

import static com.jit.defkoi.service.pref.PreferenceKey.*;

@NoArgsConstructor
public class Preferences extends CommonPreferences {

  /** Builds a set of preferences with default values */
  @Override
  public void setDefaults() {
    super.setDefaults();
    prefs.put(showRetiredDevices, new Preference(showRetiredDevices).value(Boolean.FALSE));
    prefs.put(showRetiredCapabilities, new Preference(showRetiredCapabilities).value(Boolean.FALSE));
    prefs.put(showRetiredPipeConfs, new Preference(showRetiredPipeConfs).value(Boolean.FALSE));
  }

  public Preferences user(User user) {
    super.user(user);
    this.user = user;
    setDefaults();
    for(Preference pref : prefs.values())
      pref.user(user);
    for(Preference pref : user.getPreferences()) {
      try {
        if(pref.getPrefKey() == null)
          pref.setPrefKey(PreferenceKey.valueOf(pref.getName()));
        prefs.put(pref.getPrefKey(), pref);
      } catch(IllegalArgumentException e) {
      }
    }
    return this;
  }

  @JsonProperty("showRetiredDevices")
  public boolean isShowRetiredDevices() {
    return getPreference(showRetiredDevices).getBooleanData();
  }

  @JsonProperty("showRetiredDevices")
  public void setShowRetiredDevices(boolean value) {
    setPreference(showRetiredDevices, value);
  }

  @JsonProperty("showRetiredCapabilities")
  public boolean isShowRetiredCapabilities() {
    return getPreference(showRetiredCapabilities).getBooleanData();
  }

  @JsonProperty("showRetiredCapabilities")
  public void setShowRetiredCapabilities(boolean value) {
    setPreference(showRetiredCapabilities, value);
  }

  @JsonProperty("showRetiredPipeConfs")
  public boolean isShowRetiredPipeConfs() {
    return getPreference(showRetiredPipeConfs).getBooleanData();
  }

  @JsonProperty("showRetiredPipeConfs")
  public void setShowRetiredPipeConfs(boolean value) {
    setPreference(showRetiredPipeConfs, value);
  }

}

