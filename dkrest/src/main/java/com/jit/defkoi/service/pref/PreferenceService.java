package com.jit.defkoi.service.pref;

import com.jit.defkoi.config.DefaultConfig;
import com.jit.defkoi.jpa.User;
import com.jit.defkoi.jpa.pref.Preference;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;

@Service
public class PreferenceService extends CommonPreferenceService<Preferences> {

  @Override
  public Preferences getPreferences() {
    User user = userRepo.loggedUser(true);
    if(user != null)
      return new Preferences().user(user);
    return new Preferences();
  }

  @Override
  @Transactional
  @Modifying
  public Preferences savePreferences(Preferences preferences) {
    User user = userRepo.loggedUser(true);
    Preferences saved = new Preferences().user(user);
    prefRepo.saveAll(saved.getPreferences());
    for(Preference p : preferences.getPreferences()) {
      preferences.setPreference(prefRepo.save(saved.getPreference(p.getPrefKey()).value(p.value())));
    }
    return preferences;
  }

  public Config getConfig() {
    return new Config(prefRepo.findByUser(null));
  }

  public Config getConfig(DefaultConfig defaultConfig) throws InvocationTargetException, IllegalAccessException {
    return new Config(defaultConfig, new Config(prefRepo.findByUser(null)));
  }

  @Transactional
  @Modifying
  public Config saveConfig(Config config) {
    Config saved = getConfig();
    prefRepo.saveAll(saved.getPreferences());
    for(Preference p : config.getPreferences()) {
      config.setPreference(prefRepo.save(saved.getPreference(p.getPrefKey())).value(p.value()));
    }
    return config;
  }

}

