import $ from "jquery";
import { toast } from "react-toastify";

import { restUrl } from "../App";
import { extractMessage } from "../AjaxResponse";
import "./styles.css";

export function entityPatcher(itemName, data, formKey, value) {
  let url = restUrl + itemName + "/edit/" + data.id;
  let params = {};
  params[formKey] = value;
  let promise = $.ajax({
    method: "POST",
    url: url,
    xhrFields: { withCredentials: true },
    data: params
  });
  toast.promise(promise, {
    success: "Saved",
    error: ({data}) => extractMessage(data)
  }, {position: "bottom-right"} );
  return promise;
}

