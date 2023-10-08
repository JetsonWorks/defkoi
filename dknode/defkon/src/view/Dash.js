import React, { useEffect } from 'react';
import { Tab, TabList, TabPanel, Tabs } from "react-tabs";
import PropTypes from "prop-types";

import DashCharts from "./DashCharts";
import Config from "./Config";
import { CapabilitiesPage, DevicesPage, PipeConfsPage } from "../search/SearchPages";
import "./react-tabs.css";
import "./styles.css";

export default function Dash(props) {

  useEffect(() => {
    document.title = props.title ? props.title : "Dashboard";
  }, [props.title]);

  return (
    <div>
      <h2 className="pageTitle">{props.title ? props.title : "Dashboard"}</h2>
      <DashCharts/>

      <Tabs selectedTabClassName="reactTabs-tabSelected" defaultIndex={3}>

        <TabList>
          <Tab key="config"><h4>Config</h4></Tab>
          <Tab key="devices"><h4>Devices</h4></Tab>
          <Tab key="capabilities"><h4>Capabilities</h4></Tab>
          <Tab key="pipeConfs"><h4>Pipeline Configurations</h4></Tab>
        </TabList>

        <TabPanel key="config">
          <Config/>
        </TabPanel>
        <TabPanel key="devices">
          <DevicesPage itemType="Device" itemName="device" collectionName="devices"
                       showRetiredPrefKey="showRetiredDevices" pageSizeInterval={props.pageSizeInterval}
                       enableMultiSelectTools={props.enableMultiSelectTools}
                       enableExport={props.enableExport}/>
        </TabPanel>
        <TabPanel key="capabilities">
          <CapabilitiesPage itemType="Capability" itemName="capability" collectionName="capabilities"
                            showRetiredPrefKey="showRetiredCapabilities" pageSizeInterval={props.pageSizeInterval}
                            enableMultiSelectTools={props.enableMultiSelectTools}
                            enableExport={props.enableExport}/>
        </TabPanel>
        <TabPanel key="pipeConfs">
          <PipeConfsPage itemType="PipeConf" itemName="pipeConf" collectionName="pipeConfs"
                         showRetiredPrefKey="showRetiredPipeConfs" pageSizeInterval={props.pageSizeInterval}
                         enableMultiSelectTools={props.enableMultiSelectTools}
                         enableExport={props.enableExport}/>
        </TabPanel>

      </Tabs>
    </div>
  );

}
Dash.propTypes = {
  title: PropTypes.string,
}

