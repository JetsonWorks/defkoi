import React from "react";
import { toast } from "react-toastify";

export function handleCommError() {
  toast.error("Error communicating with server", {
    autoClose: false
  });
}

export function extractResponseFields(jqXHR) {
  let fields = {
    status: 200,
    message: "OK"
  };
  if(typeof jqXHR === "string")
    return fields;
  // some endpoints return 200 with a map containing result
  if(jqXHR.result) {
    fields.message = jqXHR.result;
    return fields;
  }

  if(jqXHR.status === 0) {
    return {
      status: jqXHR.status,
      error: "Connection refused"
    }
  }
  if(jqXHR.status < 300) {
    return {
      status: jqXHR.status,
      message: jqXHR.responseText ? jqXHR.responseText : ""
    }
  }
  if(jqXHR.status >= 300)
    return {
      status: jqXHR.status,
      error: jqXHR.responseText ? jqXHR.responseText : ""
    };
  if(jqXHR.responseJSON) {
    for(let field in jqXHR.responseJSON)
      fields[field] = jqXHR.responseJSON[field];
  } else {
    if(jqXHR.status) {
      fields.status = jqXHR.status;
      fields.error = jqXHR.statusText;
    } else {
      fields.error = jqXHR.responseText ? jqXHR.responseText :
         (jqXHR.statusText && jqXHR.statusText !== "error") ? jqXHR.statusText :
         "An error has occurred";
    }
  }
  return fields;
}

export function extractMessage(jqXHR) {
  let fields = extractResponseFields(jqXHR);
  if(fields.status) {
    if(fields.status < 300)
      return fields.message;
    return fields.status + ": " + (fields.error ? fields.error : fields.message);
  }
  if(fields.generalError)
    return fields.generalError;
  if(fields.error)
    return fields.error;
  let msg = "";
    for(let field in fields)
      msg = msg + fields[field];
  return msg;
}

export function toastMessage(jqXHR) {
  let fields = extractResponseFields(jqXHR);
  if(typeof(fields.status) === "number") {
    if(fields.message) {
      toast.info((
        <div>{fields.status}: {fields.message}</div>
      ));
    }
    else {
      toast.error((
        <div>
          {fields.path &&
            <div>{fields.path}</div>
          }
          <div>{fields.status}: {fields.error}</div>
        </div>
      ));
    }
  } else if(fields.generalError) {
    toast.error(fields.generalError);
  } else if(fields.error) {
    toast.error(fields.error);
  } else {
    for(let field in fields)
      toast.error(fields[field]);
  }
}

