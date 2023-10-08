import React, { useEffect, useState } from 'react';
import $ from "jquery";
import { toast } from "react-toastify";
import classNames from "classnames";

import { useDispatch, useSelector } from 'react-redux';
import { getConfig, hasRoleOperator, selectAvailableResolutions, selectConfig, } from '../features/metadata/MdSlice';

import { restUrl } from "../App";
import { NumberField, SelectOption, Text, Toggle, } from "../field";
import "../tableview.css";

export default function Config(props) {
  const dispatch = useDispatch();
  const config = useSelector(selectConfig);
  const availRes = useSelector(selectAvailableResolutions);
  const [patch, setPatch] = useState({});
  const isOperator = useSelector(hasRoleOperator);

  useEffect(() => {
    if(props.title)
      document.title = props.title;
  }, [props.title]);

  function submit() {
    let url = restUrl + "config";
    return $.ajax({
      type: "PATCH",
      url: url,
      data: JSON.stringify(patch),
      success: function () {
        toast.success("Saved config", {position: "bottom-right"});
      },
      contentType: "application/json"
    });
  }

  function toggle(key, value) {
    let data = {...patch};
    data[key] = value;
    setPatch(data);
  }

  function changeHandler(event) {
    let data = {...patch};
    data[event.target.id] = event.target.value;
    if("checkbox" === event.target.type)
      data[event.target.id] = (event.target.checked === true);
    setPatch(data);
  }

  function save() {
    submit()
    .done(function (data, textStatus, jqXHR) {
      dispatch(getConfig());
      setPatch({});
    });
  }

  function isDirty() {
    return Object.keys(patch).length > 0;
  }

  return (
    <main>
      {props.title &&
        <h1>{props.title}</h1>
      }

      <div className="css-tableview">
        {isOperator &&
          <div>
            <button className={classNames({"side-button": true, "left": true, "active": true !== isDirty()})}>Saved</button>
            <button className={classNames({"side-button": true, "right": true, "active": true === isDirty()})} onClick={() => save()}>Save</button>
          </div>
        }
        <ul>
          <li>
            <div className="table-cell">
              <h3>General</h3>
              <ul className="vertical">
                <Toggle label="Debug" selectId="debug" className="green" disabled={!isOperator}
                        defaultValue={config.debug} value={patch.debug} onChange={toggle}/>
                <Toggle label="NV-Capable" selectId="nvCapable" className="green" disabled={true}
                        defaultValue={config.nvCapable} value={patch.nvCapable} onChange={toggle}/>
                <Text label="Media Output Directory" selectId="mediaDir" disabled={!isOperator}
                      value={config.mediaDir} onChange={changeHandler}/>
                <NumberField label="Max Images Saved" selectId="maxImages" disabled={!isOperator}
                        value={config.maxImages} onChange={changeHandler}/>
                <NumberField label="Max Videos Saved" selectId="maxVideos" disabled={!isOperator}
                        value={config.maxVideos} onChange={changeHandler}/>
                <NumberField label="Video Length" selectId="videoLength" disabled={!isOperator}
                        value={config.videoLength} onChange={changeHandler}/>
                <Text label="Labels File" selectId="labelFile" disabled={!isOperator}
                      value={config.labelFile} onChange={changeHandler}/>
                <Toggle label="Log Stats to DB" selectId="logStatsEnabled" className="green" disabled={!isOperator}
                        defaultValue={config.logStatsEnabled} value={patch.logStatsEnabled} onChange={toggle}/>
                <NumberField label="Stats Log Period" selectId="statsUpdatePeriod" disabled={!isOperator}
                        value={config.statsUpdatePeriod} onChange={changeHandler}/>
                <NumberField label="Max Queue Size" selectId="queueMaxSize" disabled={!isOperator}
                        value={config.queueMaxSize} onChange={changeHandler}/>
              </ul>
            </div>
            <div className="table-cell">
              <h3>Default Camera Config</h3>
              <ul className="vertical">
                <SelectOption label="Max Capture Resolution" selectId="maxCaptureRes" objects={availRes} disabled={!isOperator}
                              value={config.maxCaptureRes} onChange={changeHandler}/>
                <Toggle label="Tap Live Feed (xvimage)" selectId="tapLiveEnabled" className="green" disabled={!isOperator}
                        defaultValue={config.tapLiveEnabled} value={patch.tapLiveEnabled} onChange={toggle}/>
                <Toggle label="Publish live feed to RTSP" selectId="liveRtspEnabled" className="green" disabled={!isOperator}
                        defaultValue={config.liveRtspEnabled} value={patch.liveRtspEnabled} onChange={toggle}/>
                <Toggle label="Publish object images to RTSP" selectId="objectRtspEnabled" className="green" disabled={!isOperator}
                        defaultValue={config.objectRtspEnabled} value={patch.objectRtspEnabled} onChange={toggle}/>
                <Text label="RTSP Base URL" selectId="rtspProxyUrl" disabled={!isOperator}
                      value={config.rtspProxyUrl} onChange={changeHandler}/>
              </ul>
              <h3>Motion Detection</h3>
              <ul className="vertical">
                <Toggle label="Motion Detection" selectId="motDetectEnabled" className="green" disabled={!isOperator}
                        defaultValue={config.motDetectEnabled} value={patch.motDetectEnabled} onChange={toggle}/>
                <Toggle label="Use Grayscale" selectId="motionGrayscale" className="green" disabled={!isOperator}
                        defaultValue={config.motionGrayscale} value={patch.motionGrayscale} onChange={toggle}/>
                <SelectOption label="Max Motion Detection Resolution" selectId="maxMotDetectRes" objects={availRes} disabled={!isOperator}
                              value={config.maxMotDetectRes} onChange={changeHandler}/>
                <NumberField label="Object Squelch Period" selectId="objectSquelchPeriod" disabled={!isOperator}
                        value={config.objectSquelchPeriod} onChange={changeHandler}/>
              </ul>
            </div>
            <div className="table-cell">
              <h3>Object Detection</h3>
              <ul className="vertical">
                <Toggle label="Object Detection" selectId="objDetectEnabled" className="green" disabled={!isOperator}
                        defaultValue={config.objDetectEnabled} value={patch.objDetectEnabled} onChange={toggle}/>
                <Toggle label="Save Bounding Box Image" selectId="saveBoundingBoxImage" className="green" disabled={!isOperator}
                        defaultValue={config.saveBoundingBoxImage} value={patch.saveBoundingBoxImage}
                        onChange={toggle}/>
                <SelectOption label="Max Object Detection Resolution" selectId="maxObjDetectRes" objects={availRes} disabled={!isOperator}
                              value={config.maxObjDetectRes} onChange={changeHandler}/>

                <Text label="Detection Engine" selectId="engine" disabled={!isOperator}
                      value={config.engine} onChange={changeHandler}/>
                <Text label="Artifact ID" selectId="artifactId" disabled={!isOperator}
                      value={config.artifactId} onChange={changeHandler}/>
                <Text label="Backbone" selectId="backbone" disabled={!isOperator}
                      value={config.backbone} onChange={changeHandler}/>
                <Text label="Flavor" selectId="flavor" disabled={!isOperator}
                      value={config.flavor} onChange={changeHandler}/>
                <Text label="Dataset" selectId="dataset" disabled={!isOperator}
                      value={config.dataset} onChange={changeHandler}/>
                <Text label="Threshold" selectId="argThreshold" disabled={!isOperator}
                      value={config.argThreshold} onChange={changeHandler}/>
              </ul>
            </div>
          </li>
        </ul>
      </div>

    </main>
  );

}

