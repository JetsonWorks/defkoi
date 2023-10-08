import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { fetchAvailableResolutions, fetchConfig, fetchDeviceApis, fetchPrefs, fetchStats, } from './MdAPI';

const initialState = {
  status: 'idle',
  prefs: {
    loaded: false,
    burgerSide: "left",
    burgerAnimation: "push",
    burgerAutoHide: true,
    gridAnimatedRows: true,
  },
  burger: {
    menuOpen: false,
  },
  auth: {
    profile: {},
    resourceAccess: {},
  },
  filterText: null,
  selectedItems: [],
  chosenRestUrl: "",
  online: true,
  config: {},
  deviceApis: [],
  stats: {},
  availableResolutions: [],
};

export const selectFilterText = (state) => state.metadata.filterText;

export const getPrefs = createAsyncThunk(
  'md/getPrefs',
  async() => {
    const data = await fetchPrefs();
    // The value we return becomes the `fulfilled` action payload
    return data;
  }
);

export const updateOnline = createAsyncThunk(
  'md/updateOnline',
  async() => {
    return;
  }
);

export const getConfig = createAsyncThunk(
  'md/getConfig',
  async() => {
    const data = await fetchConfig();
    return data;
  }
);

export const getDeviceApis = createAsyncThunk(
  'md/getDeviceApis',
  async() => {
    const data = await fetchDeviceApis();
    return data;
  }
);

export const getStats = createAsyncThunk(
  'md/getStats',
  async(params, thunkAPI) => {
    const online = selectOnline(thunkAPI.getState());
    try {
      const data = await fetchStats();
      if(online === false)
        thunkAPI.dispatch(updateOnline(true));
      return data;
    } catch(jqXHR) {
      if(jqXHR.status && jqXHR.status === 503) {
        thunkAPI.dispatch(updateOnline(false));
      }
    }
  }
);

export const getAvailableResolutions = createAsyncThunk(
  'md/getAvailableResolutions',
  async() => {
    const data = await fetchAvailableResolutions();
    return data;
  }
);

export const selectOnline = (state) => state.metadata.online;
export const selectConfig = (state) => state.metadata.config;
export const selectPrefs = (state) => state.metadata.prefs;
export const selectBurgerMenuOpen = (state) => state.metadata.burger.menuOpen;

export const selectPreferredUsername = (state) => state.metadata.auth.profile ?
  state.metadata.auth.profile.preferred_username : false;
export const selectGivenName = (state) => state.metadata.auth.profile.given_name;
export const selectRoles = (state) => {
  const ra = state.metadata.auth.resourceAccess;
  return ra && ra[process.env.REACT_APP_oidcClientId] ? ra[process.env.REACT_APP_oidcClientId].roles : [];
}

export const selectSelectedItems = (state) => state.metadata.selectedItems;

export const hasRoleUser = (state) => {
  if(!state) return false;
  const ra = state.metadata.auth.resourceAccess;
  return ra && ra[process.env.REACT_APP_oidcClientId]?.roles.includes("ROLE_DEFKOI_USER");
}
export const hasRoleOperator = (state) => {
  if(!state) return false;
  const ra = state.metadata.auth.resourceAccess;
  return ra && ra[process.env.REACT_APP_oidcClientId]?.roles.includes("ROLE_DEFKOI_OPERATOR");
}
export const hasRoleAdmin = (state) => {
  if(!state) return false;
  const ra = state.metadata.auth.resourceAccess;
  return ra && ra[process.env.REACT_APP_oidcClientId]?.roles.includes("ROLE_DEFKOI_ADMIN");
}

export const selectChosenRestUrl = (state) => state.metadata.chosenRestUrl;
export const selectDeviceApis = (state) => state.metadata.deviceApis;
export const selectStats = (state) => state.metadata.stats;
export const selectAvailableResolutions = (state) => state.metadata.availableResolutions;

let activeStatsDaemonPeriod = 1; // seconds
let idleStatsDaemonPeriod = 10; // when lost communication
let statsDaemonPeriod = activeStatsDaemonPeriod;
export const statsDaemon = createAsyncThunk(
  'cache/statsDaemon',
  async(params, thunkAPI) => {
    thunkAPI.dispatch(getStats());
    thunkAPI.dispatch(getConfig());
    setTimeout(() => thunkAPI.dispatch(statsDaemon()), statsDaemonPeriod * 1000);
    statsDaemonPeriod = idleStatsDaemonPeriod;
  }
);

export const MdSlice = createSlice({
  name: 'md',
  initialState,
  // The `reducers` field lets us define reducers and generate associated actions
  reducers: {
    setBurgerMenuOpen: (state, action) => {
      state.burger.menuOpen = action.payload;
    },
    setBurgerSide: (state, action) => {
      state.prefs.burgerSide = action.payload;
    },
    setBurgerAnimation: (state, action) => {
      state.prefs.burgerAnimation = action.payload;
    },
    setBurgerAutoHide: (state, action) => {
      state.prefs.burgerAutoHide = action.payload;
    },
    setGridAnimatedRows: (state, action) => {
      state.prefs.gridAnimatedRows = action.payload;
    },
    setAuth: (state, action) => {
      state.auth.profile = action.payload;
      state.auth.resourceAccess = action.payload.resource_access;
    },
    setChosenRestUrl: (state, action) => {
      state.chosenRestUrl = action.payload;
    },
    setFilterText: (state, action) => {
      state.filterText = action.payload;
    },
    // updates the specified preference with the specified value
    // params: key, value
    setPref: (state, action) => {
      if(action.payload.key)
        state.prefs[action.payload.key] = action.payload.value;
    },
    // sets the items currently selected
    // params: array of items
    setSelectedItems: (state, action) => {
      state.selectedItems = action.payload;
    },
  },

  // The `extraReducers` field lets the slice handle actions defined elsewhere,
  // including actions generated by createAsyncThunk or in other slices.
  extraReducers: (builder) => {
    builder
    .addCase(updateOnline.fulfilled, (state, action) => {
      let replace = {...state};
      replace.online = action.meta.arg;
      return replace;
    })
    .addCase(getConfig.pending, (state) => {
      state.status = 'loading';
    })
    .addCase(getConfig.fulfilled, (state, action) => {
      let replace = {...state};
      replace.status = 'idle';
      if(!action.payload)
        return replace;

      replace.config = action.payload;
      return replace;
    })

    .addCase(getPrefs.pending, (state) => {
      state.status = 'loading';
    })
    .addCase(getPrefs.fulfilled, (state, action) => {
      let replace = {...state};
      replace.status = 'idle';
      if(!action.payload)
        return replace;
      replace.prefs = {...state.prefs};

      replace.prefs.burgerSide = action.payload.burgerSide;
      replace.prefs.burgerAnimation = action.payload.burgerAnimation;
      replace.prefs.burgerAutoHide = action.payload.burgerAutoHide;
      replace.prefs.gridAnimatedRows = action.payload.gridAnimatedRows;

      replace.prefs.showRetiredDevices = action.payload.showRetiredDevices;
      replace.prefs.showRetiredCapabilities = action.payload.showRetiredCapabilities;
      replace.prefs.showRetiredPipeConfs = action.payload.showRetiredPipeConfs;

      replace.prefs.loaded = true;
      return replace;
    })

    .addCase(getDeviceApis.pending, (state) => {
      state.status = 'loading';
    })
    .addCase(getDeviceApis.fulfilled, (state, action) => {
      let replace = {...state};
      replace.status = 'idle';
      if(!action.payload)
        return replace;
      replace.deviceApis = action.payload;
      return replace;
    })

    .addCase(getStats.fulfilled, (state, action) => {
      let replace = {...state};
      if(!action.payload)
        return replace;
      replace.stats = action.payload;
      statsDaemonPeriod = activeStatsDaemonPeriod;
      return replace;
    })

    .addCase(getAvailableResolutions.fulfilled, (state, action) => {
      let replace = {...state};
      if(!action.payload)
        return replace;
      replace.availableResolutions = action.payload;
      return replace;
    })

  },
});

export const {
  setBurgerMenuOpen, setBurgerSide, setBurgerAnimation, setBurgerAutoHide, setGridAnimatedRows,
  setAuth, setFilterText, setChosenRestUrl, setPref
} = MdSlice.actions;

export default MdSlice.reducer;
