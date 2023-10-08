import { configureStore } from '@reduxjs/toolkit';
import mdReducer from './features/metadata/MdSlice';
import cacheReducer from "./features/cache/CacheSlice";

export const store = configureStore({
  reducer: {
    metadata: mdReducer,
    cache: cacheReducer,
  },
});
