package com.jit.defkoi.jpa;

import com.jit.defkoi.jpa.pref.Preference;
import org.springframework.data.rest.core.config.Projection;

import java.util.Set;

@Projection(name = "excerpt", types = { User.class })
public interface UserWithRelations {

  String getName();

  default String getId() {
    return getName();
  }

  String getFirstName();

  String getLastName();

  default String getFullName() {
    return String.format("%s %s", getFirstName() == null ? "" : getFirstName(),
      getLastName() == null ? "" : getLastName());
  }

  String getNotes();

  Set<Preference> getPreferences();

  Set<UserEvent> getUserEvents();

}
