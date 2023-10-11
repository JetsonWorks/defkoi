import $ from "jquery";
import { toast } from "react-toastify";

import { extractMessage } from "./AjaxResponse";

export function makeRequest(url, params, preMsg, postMsg) {
  return doRequest("GET", url, params, preMsg, postMsg);
}

export function postRequest(url, params, preMsg, postMsg) {
  return doRequest("POST", url, params, preMsg, postMsg);
}

export function doRequest(method, url, params, preMsg, postMsg) {
  let promise = $.ajax({
    method: method,
    url: url,
    xhrFields: { withCredentials: true },
    data: params
  });
  toast.promise(promise, {
    pending: preMsg,
    success: {
      render({data}) {
        return data.result ? extractMessage(data) : postMsg;
      }
    },
    error: {
      render({data}) {
        return extractMessage(data);
      }
    },
  }, {position: "bottom-right"});
  return promise;
}

