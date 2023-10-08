import $ from "jquery";
import { restUrl } from "../../App";
import { handleCommError } from "../../AjaxResponse";

export function zuluToIso(date) {
  // our dates are stored in GMT
  return date.replace(/Z/, "-00:00");
}

export async function checkCache(cache, etype) {
  return $.ajax({
    method: "GET",
    url: restUrl + etype + "/metadata",
    data: "lastSync=" + zuluToIso(cache[etype].modTime),
  }).done(function(data, textStatus, jqXHR) {
    if(!data || data == null)
      console.error("received null data in response to url " + restUrl + "metadata");
    return data;
  }).catch(function(jqXHR, textStatus, errorThrown) {
    console.error("caught error/exception in response to url " + restUrl + "metadata");
    console.error(jqXHR);
  });
}

// Query the collection resource for more entities modified since the specified time.
// Response from the collection resource will not be paged, and will include entity IDs and collections,
// but none of the collection items will have IDs (unless you use RepositoryRestConfiguration to override, which we are)
export function fetchCacheCollection(cache, etype, modTime) {
  const time = modTime ? modTime : cache[etype].modTime;
  try {
    if(!time) {
      return queryAndCollectResource(restUrl + cache.control[etype].collectionName +
        "/search/findFirst500ByOrderByModTime",
        cache.control[etype].collectionName, collectionHandler);
    }
    return queryAndCollectResource(restUrl + cache.control[etype].collectionName +
      "/search/findFirst1000ByModTimeGreaterThanEqualOrderByModTime?date=" + zuluToIso(time),
      cache.control[etype].collectionName, collectionHandler);
  } catch(error) {
    console.error(error);
  }
}

// Response from the item resource will not contain the item ID
// (unless you use RepositoryRestConfiguration to override, which we are), but that's ok because we already have it.
// Collections will be under _embedded and each item will have an ID, but nested collection items will not (with the above exception).
// Empty collections will be omitted, but that's ok because we already have them.
export function fetchCacheItem(collectionName, item) {
  return queryAndCollectResource(restUrl + collectionName + "/" + item.id,
    item, noIdItemHandler);
}

// Query the collection resource for entities with the specified IDs.
// collectionName could be a leaf type plural name or a parent type plural name (baseRestEntities).
export function fetchCacheItems(collectionName, ids) {
  let ary = Array.from(ids);
  let url = restUrl + collectionName + "/search/findByIdIn?ids=" + ary.join(",");
  return queryAndCollectResource(url, ary, mixedCollectionHandler);
}

async function queryAndCollectResource(url, key, dataHandler) {
  let promise;
  await $.ajax({
    method: "GET",
    url: url
  }).done(function(data, textStatus, jqXHR) {
    if(data == null) {
      return handleCommError();
    }
    if(typeof dataHandler === "function")
      promise = Promise.resolve(dataHandler(key, data));
  }).catch(function(jqXHR, textStatus, errorThrown) {
    console.error("caught error/exception in response to url " + url);
    console.error(jqXHR);
  });
  return promise;
}

function collectionHandler(key, data) {
  let collection = [];
  if(data._embedded) {
    if(!data._embedded[key])
      throw new Error("Did not find collection under _embedded." + key);
    collection = data._embedded[key];
  }
  return collection;
}

// the key is ignored because all the collections (data._embedded) are returned
// returns a collection of collections (by key)
function mixedCollectionHandler(key, data) {
  let collection = data._embedded;
  return collection;
}

// We already have the ID, so we're just filling in the details,
// but the data does not include the ID, so we can't just return the data
// (unless you use RepositoryRestConfiguration to override, which we are).
// item is the current entity, so copy properties from item, then from data, and returned the merged object.
// Note: collections will be under _embedded when not using a projection
function noIdItemHandler(item, data) {
  let value = {...item};
  Object.keys(data).filter(x => x !== "_embedded").forEach(x => value[x] = data[x]);
  if(data._embedded)
    Object.keys(data._embedded).forEach(x => value[x] = data._embedded[x]);
  return value;
}

