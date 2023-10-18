package com.jit.defkoi.service.pref;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jit.defkoi.jpa.User;
import com.jit.defkoi.jpa.pref.IPrefKey;
import com.jit.defkoi.jpa.pref.Preference;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;
import java.util.Hashtable;

import static com.jit.defkoi.service.pref.CommonPreferenceKey.*;

@NoArgsConstructor
public class CommonPreferences {

  @JsonIgnore
  protected Hashtable<IPrefKey, Preference> prefs = new Hashtable<>();

  @JsonIgnore
  protected User user;

  /** Builds a set of preferences with default values */
  public void setDefaults() {
    prefs.put(burgerSide, new Preference(burgerSide).value("left"));
    prefs.put(burgerAnimation, new Preference(burgerAnimation).value("push"));
    prefs.put(burgerAutoHide, new Preference(burgerAutoHide).value(Boolean.TRUE));
    prefs.put(gridAnimatedRows, new Preference(gridAnimatedRows).value(Boolean.TRUE));
  }

  public <T extends CommonPreferences> T user(User user) {
    this.user = user;
    setDefaults();
    for(Preference pref : prefs.values())
      pref.user(user);
    for(Preference pref : user.getPreferences()) {
      try {
        if(pref.getPrefKey() == null)
          pref.setPrefKey(CommonPreferenceKey.valueOf(pref.getName()));
        prefs.put(pref.getPrefKey(), pref);
      } catch(IllegalArgumentException e) {
      }
    }
    return (T)this;
  }

  @JsonIgnore
  public Collection<Preference> getPreferences() {
    return prefs.values();
  }

  public Preference getPreference(IPrefKey key) {
    Preference pref = prefs.get(key);
    if(pref == null) {
      pref = new Preference(key).user(user);
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
      pref = new Preference(key).user(user);
      prefs.put(key, pref);
    }
    return pref.value(value);
  }

  public void setPreference(Preference preference) {
    if(preference.getPrefKey() == null)
      preference.setPrefKey(CommonPreferenceKey.valueOf(preference.getName()));
    prefs.put(preference.getPrefKey(), preference);
  }

  public <T extends CommonPreferences> T patch(T update) {
    for(Preference c : update.getPreferences())
      if(c.value() != null)
        setPreference(c);
    return (T)this;
  }

  @JsonProperty("burgerSide")
  public String getBurgerSide() {
    return getPreference(burgerSide).getStringData();
  }

  @JsonProperty("burgerSide")
  public void setBurgerSide(String value) {
    setPreference(burgerSide, value);
  }

  @JsonProperty("burgerAnimation")
  public String getBurgerAnimation() {
    return getPreference(burgerAnimation).getStringData();
  }

  @JsonProperty("burgerAnimation")
  public void setBurgerAnimation(String value) {
    setPreference(burgerAnimation, value);
  }

  @JsonProperty("burgerAutoHide")
  public boolean getBurgerAutoHide() {
    return getPreference(burgerAutoHide).getBooleanData();
  }

  @JsonProperty("burgerAutoHide")
  public void setBurgerAutoHide(boolean value) {
    setPreference(burgerAutoHide, value);
  }

  @JsonProperty("gridAnimatedRows")
  public boolean getGridAnimatedRows() {
    return getPreference(gridAnimatedRows).getBooleanData();
  }

  @JsonProperty("gridAnimatedRows")
  public void setGridAnimatedRows(boolean value) {
    setPreference(gridAnimatedRows, value);
  }

}

