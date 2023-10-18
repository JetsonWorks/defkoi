package com.jit.defkoi.service.pref;

import com.jit.defkoi.jpa.pref.IPrefKey;
import com.jit.defkoi.jpa.pref.PrefDataType;

public enum CommonPreferenceKey implements IPrefKey {

  burgerSide {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  burgerAnimation {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  burgerAutoHide {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  gridAnimatedRows {
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
