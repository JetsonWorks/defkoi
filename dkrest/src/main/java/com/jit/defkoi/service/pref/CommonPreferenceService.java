package com.jit.defkoi.service.pref;

import com.jit.defkoi.jpa.User;
import com.jit.defkoi.jpa.UserRepository;
import com.jit.defkoi.jpa.pref.Preference;
import com.jit.defkoi.jpa.pref.PreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommonPreferenceService<T extends CommonPreferences> {

  @Autowired
  protected UserRepository userRepo;
  @Autowired
  protected PreferenceRepository prefRepo;

  public T getPreferences() {
    User user = userRepo.loggedUser(true);
    if(user != null)
      return new CommonPreferences().user(user);
    return (T)new CommonPreferences();
  }

  @Transactional
  @Modifying
  public T savePreferences(T preferences) {
    User user = userRepo.loggedUser(true);
    T saved = new CommonPreferences().user(user);
    for(Preference p : preferences.getPreferences()) {
      preferences.setPreference(prefRepo.save(saved.getPreference(p.getPrefKey()).value(p.value())));
    }
    return preferences;
  }

}

