import React, { useState } from 'react';
import $ from "jquery";
import { Route, Routes } from "react-router";
import { ToastContainer } from "react-toastify";
import { withCookies } from "react-cookie";
import { useAuth } from "react-oidc-context"
import { useDispatch, useSelector } from 'react-redux';

import Burger from "./menu/Burger";
import Status from "./view/Status";
import Config from "./view/Config";
import Preferences from "./view/Preferences";
import Dash from "./view/Dash";
import { CapabilitiesPage, DevicesPage, PipeConfsPage } from "./search/SearchPages";

import "./App.scss";
import { getAvailableResolutions, getConfig, getDeviceApis, getPrefs, hasRoleOperator, setAuth, startStatsDaemon } from "./features/metadata/MdSlice";
import { startRefreshDaemon } from "./features/cache/CacheSlice";

export const restUrl = process.env.REACT_APP_restUrl + "model/";

function App(props) {
  const dispatch = useDispatch();
  const auth = useAuth();
  const {cookies} = props;
  const [isBooted, setBooted] = useState(false);

  $.ajaxSetup({
    beforeSend: function(xhr, settings) {
      if(auth.isAuthenticated) {
        xhr.setRequestHeader("Accept", "application/json");
        xhr.setRequestHeader("Authorization", "Bearer " + auth.user.access_token);
      }
      if(settings.type === 'POST' || settings.type === 'PATCH' || settings.type === 'DELETE') {
        // Only send the token to our URLs
        if(/model/.test(settings.url)) {
          if(cookies.get("DKREST-XSRF")) {
            xhr.setRequestHeader("X-DKREST-XSRF", cookies.get('DKREST-XSRF'));
          }
        }
      }
    }
  });

  const isOperator = useSelector(hasRoleOperator);

  if(auth.isLoading) {
    return <div>Loading...</div>;
  }

  if(auth.error) {
    return <div>Oops... {auth.error.message}</div>;
  }

  if(auth.isAuthenticated) {
    if(isBooted === true)
      return outerContainer();

    dispatch(setAuth(auth.user.profile));
    dispatch(getPrefs());
    dispatch(startRefreshDaemon());
    dispatch(startStatsDaemon());
    dispatch(getConfig());
    dispatch(getDeviceApis());
    dispatch(getAvailableResolutions());
    setBooted(true);
  }

  return renderLogin();

  function renderLogin() {
    return (
      <div className="loginGreeting">
        <h2><a href="#" onClick={() => auth.signinRedirect()}>Please Click Here To Log In</a></h2>
      </div>
    );
  }

  function outerContainer() {
    return (
      <div id="outer-container">
        <img className="logo" src={require(process.env.REACT_APP_logo)} alt="DefKoi Logo"/>
        <Burger overlay={false} pageWrapId="mainContent"/>
        <div id="mainContent">
          <Routes>

            {isOperator &&
              <Route path="/status" element={<Status title="Status"/>}/>
            }

            <Route path="/" element={
              <Dash title="Dashboard" pageSizeInterval={50} enableMultiSelectTools={true} enableExport={true}/>
            }/>

            <Route path="/config" element={
              <Config title="Config"/>
            }/>

            <Route path="/devices" element={
              <DevicesPage title="Devices" itemType="Device" itemName="device" collectionName="devices"
                           showRetiredPrefKey="showRetiredDevices" pageSizeInterval={50}
                           enableMultiSelectTools={true}
                           enableExport={true}/>
            }/>
            <Route path="/capabilities" element={
              <CapabilitiesPage title="Capabilities" itemType="Capability" itemName="capability" collectionName="capabilities"
                                showRetiredPrefKey="showRetiredCapabilities" pageSizeInterval={50}
                                enableMultiSelectTools={true}
                                enableExport={true}/>
            }/>
            <Route path="/pipeConfs" element={
              <PipeConfsPage title="PipeConfs" itemType="PipeConf" itemName="pipeConf" collectionName="pipeConfs"
                             showRetiredPrefKey="showRetiredPipeConfs" pageSizeInterval={50}
                             enableMultiSelectTools={true}
                             enableExport={true}/>
            }/>

            <Route path="/preferences" element={
              <Preferences title="Preferences"/>
            }/>

          </Routes>
        </div>
        <ToastContainer
          autoClose={3000}
        />
      </div>
    );
  }
}

export default withCookies(App);

