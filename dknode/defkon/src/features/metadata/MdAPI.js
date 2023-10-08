import $ from "jquery";
import { restUrl } from "../../App";
import { toastMessage } from "../../AjaxResponse";

export function fetchPrefs() {
  return queryAndCollectResource(restUrl + "prefs");
}

function queryAndCollectResource(url) {
  return $.ajax({
    method: "GET",
    url: url
  }).done(function(data, textStatus, jqXHR) {
    if(data == null) {
      console.error("received null data in response to url " + url);
    }
  }).catch(function(jqXHR, textStatus, errorThrown) {
    toastMessage(jqXHR);
    console.error("caught error/exception in response to url " + url);
    console.error(jqXHR);
  });
}

export function fetchConfig() {
  return queryAndCollectResource(restUrl + "config");
}

export function fetchDeviceApis() {
  return queryAndCollectResource(restUrl + "device/apis");
}

export function fetchStats() {
  return queryAndCollectResource(restUrl + "stats");
}

export function fetchAvailableResolutions() {
  return queryAndCollectResource(restUrl + "availableResolutions");
}

