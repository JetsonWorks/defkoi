package com.jit.defkoi.service.pref;

import com.jit.defkoi.jpa.pref.IPrefKey;
import com.jit.defkoi.jpa.pref.PrefDataType;

public enum PreferenceKey implements IPrefKey {

  showRetiredDevices {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  showRetiredCapabilities {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  showRetiredPipeConfs {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

}
