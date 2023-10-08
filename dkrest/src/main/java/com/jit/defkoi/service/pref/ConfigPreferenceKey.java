package com.jit.defkoi.service.pref;

import com.jit.defkoi.jpa.pref.IPrefKey;
import com.jit.defkoi.jpa.pref.PrefDataType;

public enum ConfigPreferenceKey implements IPrefKey {

  // General Config

  debug {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  mediaDir {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  maxVideos {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  videoLength {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  labelFile {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  logStatsEnabled {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  statsUpdatePeriod {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  nvCapable {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  maxImages {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  queueMaxSize {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  rtspProxyUrl {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  tapLiveEnabled {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  liveRtspEnabled {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  objectRtspEnabled {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  // Default Camera Config

  vidSource {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  camUri {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  v4l2Device {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  maxCaptureWidth {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  maxCaptureHeight {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  // Detection Config

  objDetectEnabled {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  maxObjDetectWidth {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  maxObjDetectHeight {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  motDetectEnabled {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  motionGrayscale {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.BOOLEAN;
    }
  },

  maxMotDetectWidth {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  maxMotDetectHeight {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  objectSquelchPeriod {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  engine {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  artifactId {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  backbone {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  flavor {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  dataset {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.STRING;
    }
  },

  argThreshold {
    @Override
    public String getName() {
      return name();
    }

    @Override
    public PrefDataType getDataType() {
      return PrefDataType.NUMERIC;
    }
  },

  saveBoundingBoxImage {
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
