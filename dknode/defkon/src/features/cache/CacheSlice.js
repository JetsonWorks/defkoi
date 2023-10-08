import { createAsyncThunk, createSlice } from '@reduxjs/toolkit';

import { checkCache, fetchCacheCollection, fetchCacheItem, fetchCacheItems } from './CacheAPI';

let initialState = {
  status: "idle",
  control: {
    device: {
      collectionName: "devices",
      l2cache: "baseRestEntity",
    },
    capability: {
      collectionName: "capabilities",
      l2cache: "baseRestEntity",
    },
    pipeConf: {
      collectionName: "pipeConfs",
      l2cache: "baseRestEntity",
    },
  },
  noCache: [],
};

// entity by ID, per type - for faster processing, but not stored in the state
let cacheSet = {};

// entity IDs that are currently being fetched (item resource), per type
let fetchingItems = {};

// build a state entity cache object for each type in control and each type's secondary cache
let l2caches = new Set();
Object.keys(initialState.control).forEach(etype => {
  let cache = {
    status: "",
    fetchingCollection: false,
    modTime: "1970-01-01T00:00:00.000Z",
    touchTime: null,
    entities: [],
  };
  initialState[etype] = cache;
  cacheSet[etype] = new Set();
  fetchingItems[etype] = new Set();
  if(initialState.control[etype].l2cache) {
    l2caches.add(initialState.control[etype].l2cache);
  }
});
l2caches.forEach(l2 => {
  cacheSet[l2] = new Set();
  fetchingItems[l2] = new Set();
});

// map reference/property names to their types - for faster lookup
let byCollName = new Map();
Object.keys(initialState.control).forEach(etype => {
  byCollName.set(initialState.control[etype].collectionName, etype);
});

// these caches are loaded once we receive the cache metadata
const preFetchCaches = ["device", "capability", "pipeConf"];

let activeEntityCacheTimeouts = new Map();
let activeTimeout = 900; // seconds
let refreshDaemonPeriod = 10; // seconds
let preFetchCacheIdleTrigger = 20; // seconds

// we don't store unserializable objects (Map) in the state, so store the sorted entity values
function sortSerialEntities(emap) {
  let array = [];
  emap.forEach((value, key, map) => {
    array.push({...value});
  });
  return array.sort((a, b) => {
    if(!a.name) return -1;
    if(!b.name) return 1;
    return a.name.localeCompare(b.name);
  });
}

export const selectCache = (state) => state.cache;

function handleRejection(state, action) {
  if(action.payload && action.payload.errorMessage) {
    state.error = action.payload.errorMessage
    console.error(state.error);
  } else {
    state.error = action.error.message;
    if(action.type) console.error("Rejection: " + action.type);
    console.error(state.error);
    if(action.error.stack) console.error(action.error.stack);
  }
}

export const selectControl = (state) => state.cache.control;

// returns true if we're up-to-date (according to the control modTime)
function isCacheCurrent(cache, type, controlModTime) {
  return cache[type].modTime && controlModTime <= cache[type].modTime;
}

// Entity types may have different ways of tracking mod time
function getPayloadEntityModTime(pent) {
  return pent.modTime;
}

// updates the cache status with percentage of all entities cached
function calcCacheProgress(cache) {
  let total = 0, count = 0;
  Object.keys(cache.control).forEach(key => {
    if(!cache.noCache.includes(key)) {
      let ct = cache.control[key].count ? cache.control[key].count : 0;
      total += ct;
      count += cache[key].entities.length;
    }
  });
  if(count > 0)
    cache.status = (count / total * 100).toFixed(0) + "% cached";
  else
    cache.status = "0% cached";
}

// true if all specified cache types are fully loaded
export function areCachesLoaded(cache, types) {
  let total = 0, count = 0;
  types.forEach(type => {
    let ct = cache.control[type].count ? cache.control[type].count : 0;
    let cc = cache[type].entities.length;
    total += ct;
    count += cc;
    if(cc < ct)
      console.debug("Cache " + type + " loaded " + cc + " / " + ct);
  });
  return total <= count;
}

// returns array of IDs of entities that are not loaded
export function entitiesNotLoaded(type, entityIds) {
  return entityIds.filter(x => !cacheSet[type].has(x));
}

// use this to process results from collection resource
// caches the response payload and updates the cache metadata
function indexCollection(cache, type, payload, onDemand) {
  try {
    if(!payload) return;
    let ecache = cache[type];
    let ecset = cacheSet[type];
    let scset = cacheSet[initialState.control[type].l2cache];
    let ecmap = mapEntities(ecache.entities);
    let maxModTime = ecache.modTime ? ecache.modTime.replace(/Z/, "+00:00") : null;

    if(payload.length === 0) {
      ecache.fetchingCollection = false;
      return;
    }

    payload.forEach(pent => {
      let entity = ecmap.get(pent.id);
      // entity could be a proxy, and sometimes the target is undefined
      if(!entity || !entity.id) {
        ecmap.set(pent.id, pent);
      } else {
        entity = {...entity};
        ecmap.set(pent.id, entity);
        // replace all of the cached entity's properties
        Object.keys(pent).forEach(key => entity[key] = pent[key]);
      }
      ecset.add(pent.id);
      if(scset) {
        scset.add(pent.id);
      }
      if(!maxModTime || getPayloadEntityModTime(pent) > maxModTime)
        maxModTime = getPayloadEntityModTime(pent);
    });

    // if we are indexing results from an ad-hoc query, don't change our modTime progress marker
    if(!onDemand)
      ecache.modTime = maxModTime.replace(/\+.*/, "Z");

    ecache.entities = sortSerialEntities(ecmap);
    ecache.touchTime = new Date().toISOString();
    let ct = cache.control[type].count ? cache.control[type].count : 0;
    let cc = ecache.entities.length;
    if(cc >= ct)
      ecache.fetchingCollection = false;
    ecache.status = "loaded " + cc + "/" + ct;
  } catch(err) {
    console.error(err);
  }
}

// use this to process results from search
// similar to indexCollection, but it does not update cache modTime and it will replace entity properties
function cacheCollection(cache, type, payload) {
  try {
    if(!payload || payload.length === 0)
      return;
    let ecache = cache[type];
    let ecset = cacheSet[type];
    let scset = cacheSet[initialState.control[type].l2cache];
    let ecmap = mapEntities(ecache.entities);
    payload.forEach(pent => {
      if(!pent.id) throw new Error("Warning: entity of type " + type + " does not have an ID, so we cannot cache");
      // in case the active flag is not correct
      if(pent.retireTime !== undefined) {
        pent.active = new Date(pent.retireTime).getTime() > new Date().getTime();
      }
      let entity = ecmap.get(pent.id);
      // entity could be a proxy, and sometimes the target is undefined
      if(!entity || !entity.id) {
        ecmap.set(pent.id, pent);
      } else {
        entity = {...entity};
        ecmap.set(pent.id, entity);
        // replace all of the cached entity's properties
        Object.keys(pent).forEach(key => entity[key] = pent[key]);
      }
      ecset.add(pent.id);
      if(scset) {
        scset.add(pent.id);
      }
    });
    ecache.entities = sortSerialEntities(ecmap);
    ecache.touchTime = new Date().toISOString();
  } catch(err) {
    console.error(err);
  }
}

// return a map of entities by ID
function mapEntities(array) {
  let emap = new Map();
  array.forEach(e => {
    emap.set(e.id, e);
  });
  return emap;
}

// returns an entity or an array of the specified entities from cache
export function getFromCache(entities, cache) {
  if(Array.isArray(entities)) {
    let cached = [];
    for(let e of entities) {
      for(let type of Object.keys(cache.control)) {
        // user.id is not always populated (e.g. Document.creator) but user.name is same as user.id
        let key = e.id ? e.id : e.name;
        if(cacheSet[type].has(key)) {
          cached.push(cache[type].entities.find(x => x.id === key));
          break;
        }
      }
    }
    return cached;
  }
  let e = entities;
  for(let type of Object.keys(cache.control)) {
    let key = e.id ? e.id : e.name;
    if(cacheSet[type].has(key)) {
      return cache[type].entities.find(x => x.id === key);
    }
  }
  return null;
}

export const refreshDaemon = createAsyncThunk(
  'cache/refreshDaemon',
  async(params, thunkAPI) => {
    const cache = selectCache(thunkAPI.getState());
    try {
      let anyFetching = false;
      let maxRemaining = 0;
      Object.keys(initialState.control).forEach(type => {
        let remaining = activeEntityCacheTimeouts.get(type);
        anyFetching = anyFetching || cache[type].fetchingCollection === true;
        if(remaining)
          maxRemaining = Math.max(maxRemaining, remaining);
        if(remaining && remaining > 0) {
          // if there is no collection resource, that means this is not an entity type
          if(initialState.control[type].collectionName) {
            // console.debug("dispatching(refreshCache({" + type + ", true}))");
            thunkAPI.dispatch(refreshCache({type: type, internal: true}));
            activeEntityCacheTimeouts.set(type, remaining - refreshDaemonPeriod);
          }
        }
      });

      // if all requested caches have been loaded and user has been idle for x seconds, pre-fetch caches
      if(maxRemaining > 0 && (activeTimeout - maxRemaining) > preFetchCacheIdleTrigger && !anyFetching) {
        preFetchCaches.forEach(type => {
          thunkAPI.dispatch(refreshCache({type: type, internal: true}));
        });
      }
    } catch(err) {
      console.error(err);
    }
    setTimeout(() => thunkAPI.dispatch(refreshDaemon()), refreshDaemonPeriod * 1000);
  }
);

// call refreshCache for all caches, starting with caches listed in preFetchCaches
export const refreshCaches = createAsyncThunk(
  'cache/refreshCaches',
  async(params, thunkAPI) => {
    preFetchCaches.forEach(type => {
      thunkAPI.dispatch(refreshCache({type: type}));
    });
    Object.keys(initialState.control).forEach(type => {
      if(!preFetchCaches.includes(type)) {
        thunkAPI.dispatch(refreshCache({type: type}));
      }
    });
  }
);

// query model/<itemName>/metadata, remove deleted items from our cache, and
// if the modTime of the server's entity cache is newer, request updated entities
// params: type, internal (optional internal flag)
export const refreshCache = createAsyncThunk(
  'cache/refreshCache',
  async(params, thunkAPI) => {
    let data;
    const cache = selectCache(thunkAPI.getState());
    if(!params.internal)
      activeEntityCacheTimeouts.set(params.type, activeTimeout);
    try {
      data = await checkCache(cache, params.type);
      if(data && !isCacheCurrent(cache, params.type, data.modTime) && !cache[params.type].fetchingCollection) {
        // console.debug("dispatching(refreshCacheCollection({type: " + params.type + "}))");
        thunkAPI.dispatch(refreshCacheCollection({type: params.type}));
      }
      if(data.deleted && data.deleted.length > 0) {
        // console.debug("dispatching(removeEntities({type: " + params.type + ", ids: " + data.deleted + ", modTime: " + data.modTime + "}))");
        thunkAPI.dispatch(removeEntities({type: params.type, ids: data.deleted, modTime: data.modTime}));
      }
    } catch(err) {
      console.error(err);
    }
    return data;
  }
);

// chain requests until we have cached all the entities
// don't bother checking the control modTime
// params: type, modTime (optional)
export const refreshCacheCollection = createAsyncThunk(
  'cache/refreshCacheCollection',
  async(params, thunkAPI) => {
    const cache = selectCache(thunkAPI.getState());
    if(cache.noCache.includes(params.type)) return;
    const data = await fetchCacheCollection(cache, params.type, params.modTime);
    if(!data) return;

    let ct = cache.control[params.type].count;
    let cc = cache[params.type].entities.length;
    try {
      if(data.length > 0 && (!ct || data.length + cc < ct)) {
        // console.debug("dispatching(refreshCacheCollection({type: " + params.type +
        //   ", modTime: " + params.modTime + getPayloadEntityModTime(data[data.length - 1]).replace(/\+.*/, "Z") +
        //   ", id: " + data[data.length - 1].id, "}))");
        thunkAPI.dispatch(refreshCacheCollection({
          type: params.type,
          modTime: getPayloadEntityModTime(data[data.length - 1]).replace(/\+.*/, "Z"),
        }));
      }

    } catch(err) {
      console.error(err);
    }

    return data;
  }
);

// params: type, entity
export const refreshCacheItem = createAsyncThunk(
  'cache/refreshCacheItem',
  async(params, thunkAPI) => {
    const cache = selectCache(thunkAPI.getState());
    if(fetchingItems[params.type].has(params.entity.id)) return;
    fetchingItems[params.type].add(params.entity.id);
    try {
      return await fetchCacheItem(cache.control[params.type].collectionName, params.entity);
    } catch(err) {
      console.error(err);
    }
  }
);

// fetches specified items that are not already cached
// params: type, entityIds (array)
export const checkCacheItems = createAsyncThunk(
  'cache/checkCacheItems',
  async(params, thunkAPI) => {
    const cache = selectCache(thunkAPI.getState());
    let collName;
    if(cache.control[params.type]) collName = cache.control[params.type].collectionName;
    else if(params.type === "baseRestEntity") collName = "baseRestEntities";
    else throw new Error("checkCacheItems argument for type must either be a leaf entity type: 'baseRestEntity'");

    let missing = new Set();
    params.entityIds.filter(x => !cacheSet[params.type].has(x)).filter(x => !fetchingItems[params.type].has(x)).forEach(id => missing.add(id));
    if(missing.size > 0) {
      missing.forEach(m => fetchingItems[params.type].add(m));
      return await fetchCacheItems(collName, missing);
    }
  }
);

// create/update cache with response from our REST handler
// if the specified cache is outdated, refresh all metadata and this cache
// params: type, entities
export const storeSearchedEntities = createAsyncThunk(
  'cache/storeSearchedEntities',
  async(params, thunkAPI) => {
    const cache = selectCache(thunkAPI.getState());
    let maxPayloadModTime;
    params.entities.forEach(pent => {
      if(getPayloadEntityModTime(pent)) {
        let modTime = getPayloadEntityModTime(pent).replace(/\+.*/, "Z");
        if(!maxPayloadModTime || modTime > maxPayloadModTime)
          maxPayloadModTime = modTime;
      }
    });
    if(maxPayloadModTime > cache.control[params.type].modTime) {
      // console.debug("dispatching(refreshCacheCollection({type: " + params.type + "})");
      thunkAPI.dispatch(refreshCacheCollection({type: params.type}));
    }
  }
);

export const CacheSlice = createSlice({
  name: 'cache',
  initialState,
  reducers: {
    // locates the entity in our cache, updates the specified key with the specified value, and updates Redux state
    // params: type, id, dataKey, value
    updateEditedEntity: (state, action) => {
      const ecache = state[action.payload.type];
      const i = ecache.entities.findIndex(x => x.id === action.payload.id);
      const entity = {...ecache.entities[i]};
      if(!entity) {
        throw new Error("Error: We just tried to edit an entity of type " + action.payload.type + " that wasn't in our cache");
      }
      try {
        entity[action.payload.dataKey] = action.payload.value;
        ecache.touchTime = new Date().toISOString();
        ecache.entities[i] = entity;
      } catch(err) {
        console.error(err);
      }
    },

    // removes the entity from our cache
    // params: type, id
    removeEntity: (state, action) => {
      const ecache = state[action.payload.type];
      const i = ecache.entities.findIndex(x => x.id === action.payload.id);
      if(i > -1)
        ecache.entities.splice(i, 1)
    },

    // removes the entities from our cache and updates the cache's modTime
    // params: type, ids
    removeEntities: (state, action) => {
      const ecache = state[action.payload.type];
      action.payload.ids.forEach(id => {
        const i = ecache.entities.findIndex(x => x.id === id);
        if(i > -1)
          ecache.entities.splice(i, 1)
      });
      // we don't want to interrupt caching in progress, so only bump the modTime if we are fully cached
      let ct = state.control[action.payload.type].count;
      if(ecache.entities.length === ct) {
        let modTime = action.payload.modTime.replace(/\+.*/, "Z");
        if(modTime > ecache.modTime)
          ecache.modTime = modTime;
      }
    },
  },

  // The `extraReducers` field lets the slice handle actions defined elsewhere,
  // including actions generated by createAsyncThunk or in other slices.
  extraReducers: (builder) => {
    builder
    .addCase(refreshCaches.pending, (state) => {
      try {
        state.status = "loading";
      } catch(err) {
        console.error(err);
      }
    })
    .addCase(refreshCaches.fulfilled, (state, action) => {
      state.status = "idle";
    })
    .addCase(refreshCaches.rejected, handleRejection)

    .addCase(refreshCache.fulfilled, (state, action) => {
      if(!action.payload) return;
      if(state.status !== "idle")
        state.status = "idle";
      let type = action.meta.arg.type;

      if(!isCacheCurrent(state, type, action.payload.modTime)) {
        state.control[type].count = action.payload.count;
        calcCacheProgress(state);
        Object.keys(state.control).forEach(type => {
          if(state[type].status === "") state[type].status = "loaded 0/" + state.control[type].count;
        });
      }
    })
    .addCase(refreshCache.rejected, handleRejection)

    .addCase(refreshCacheCollection.pending, (state, action) => {
      state[action.meta.arg.type].fetchingCollection = true;
    })
    .addCase(refreshCacheCollection.fulfilled, (state, action) => {
      if(!action.payload) return;
      let type = action.meta.arg.type;
      indexCollection(state, type, action.payload);
      calcCacheProgress(state);
    })
    .addCase(refreshCacheCollection.rejected, handleRejection)

    .addCase(refreshCacheItem.pending, (state, action) => {
    })
    .addCase(refreshCacheItem.fulfilled, (state, action) => {
      if(!action.payload) return;
      const entity = action.payload;
      fetchingItems[action.meta.arg.type].delete(entity.id);
      try {
        const ecache = state[action.meta.arg.type];
        const i = ecache.entities.findIndex(x => x.id === entity.id);
        ecache.entities[i] = entity;
        ecache.touchTime = new Date().toISOString();
        cacheSet[action.meta.arg.type].add(entity.id);
      } catch(err) {
        console.error(err);
      }
    })
    .addCase(refreshCacheItem.rejected, (state, action) => {
      const entity = action.payload;
      fetchingItems[action.meta.arg.type].delete(entity.id);
      handleRejection(state, action);
    })

    .addCase(checkCacheItems.pending, (state, action) => {
    })
    .addCase(checkCacheItems.fulfilled, (state, action) => {
      // why would action.payload be undefined? Why is this fulfilled before the awaited response from CacheAPI?
      // I don't know, but this will be called again with a payload
      if(!action.payload) return;
      action.meta.arg.entityIds.forEach(x => fetchingItems[action.meta.arg.type].delete(x));
      // cache in the map for the actual type
      Object.keys(action.payload).forEach(collName => {
        if(byCollName.get(collName))
          indexCollection(state, byCollName.get(collName), action.payload[collName], true);
      });
      calcCacheProgress(state);
    })
    .addCase(checkCacheItems.rejected, (state, action) => {
      action.meta.arg.entityIds.forEach(x => fetchingItems[action.meta.arg.type].delete(x));
      handleRejection(state, action);
    })

    // update the cache with the search results while we wait for better copies from the collection resource
    .addCase(storeSearchedEntities.pending, (state, action) => {
      cacheCollection(state, action.meta.arg.type, action.meta.arg.entities);
    })
    .addCase(storeSearchedEntities.rejected, handleRejection)

  },
});

export const {updateEditedEntity, removeEntity, removeEntities} = CacheSlice.actions;

export default CacheSlice.reducer;

