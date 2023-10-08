import React, { useState } from 'react';
import { useSelector } from 'react-redux';
import { Bar, BarChart, BarLabel, BarSeries, DiscreteLegend, DiscreteLegendEntry, Line, LinearYAxis, LineChart, LineSeries } from 'reaviz';
import "./react-tabs.css";
import "./styles.css";

import { selectStats, } from '../features/metadata/MdSlice';

export default function DashCharts(props) {
  const channels = useSelector(selectStats);

  const [onAccept, setOnAccept] = useState(true);
  const [onConversion, setOnConversion] = useState(true);
  const [onProcess, setOnProcess] = useState(true);
  const [onDetected, setOnDetected] = useState(true);

  function createCharts() {
    let chartsByChannel = new Map();
    Object.keys(channels).forEach(cam => {
      if(channels[cam].length > 0)
        chartsByChannel.set(cam, transpose(channels[cam]));
    });
    return chartsByChannel
  }

  function transpose(channel) {
    let channelCharts = {
      motion: {
        key: "MotionRate",
        colors: {
          accept: '#888888',
          conversion: '#aaffaa',
          process: 'lime',
          detected: 'darkgreen',
        },
        data: [{
          key: "accept",
          data: [],
        }, {
          key: "conversion",
          data: [],
        }, {
          key: "process",
          data: [],
        }, {
          key: "detected",
          data: [],
        }],
      },
      motionCost: {
        key: "MotionCost",
        colors: {
          conv: '#aaffaa',
          proc: 'lime',
        },
        data: [{
          key: "conv",
        }, {
          key: "proc",
        }],
      },
      object: {
        key: "ObjectRate",
        colors: {
          accept: '#888888',
          conversion: '#aaaaff',
          process: 'aqua',
          detected: 'darkblue'
        },
        data: [{
          key: "accept",
          data: [],
        }, {
          key: "conversion",
          data: [],
        }, {
          key: "process",
          data: [],
        }, {
          key: "detected",
          data: [],
        }],
      },
      objectCost: {
        key: "ObjectCost",
        colors: {
          conv: '#aaaaff',
          proc: 'aqua',
        },
        data: [{
          key: "conv",
        }, {
          key: "proc",
        }],
      },
      publish: {
        key: "PublishRate",
        colors: {
          accept: '#888888',
          process: 'purple',
        },
        data: [{
          key: "accept",
          data: [],
        }, {
          key: "process",
          data: [],
        }],
      },
      objPubCost: {
        key: "PublishCost",
        colors: {
          proc: 'purple',
        },
        data: [{
          key: "proc",
        }],
      },
    };
    channel.forEach(stat => {
      // motion
      let series = channelCharts.motion.data;
      if(true === onAccept) {
        series[0].data.push({
          key: new Date(stat.timeStamp),
          data: stat.motAcceptRate,
        });
      }
      if(true === onConversion) {
        series[1].data.push({
          key: new Date(stat.timeStamp),
          data: stat.motConvRate,
        });
      }
      if(true === onProcess) {
        series[2].data.push({
          key: new Date(stat.timeStamp),
          data: stat.motProcRate,
        });
      }
      if(true === onDetected) {
        series[3].data.push({
          key: new Date(stat.timeStamp),
          data: stat.motFramesDetected,
        });
      }
      if(true === onConversion)
        channelCharts.motionCost.data[0].data = stat.motConvCost;
      if(true === onProcess)
        channelCharts.motionCost.data[1].data = stat.motProcCost;

      // predict
      series = channelCharts.object.data;
      if(true === onAccept) {
        series[0].data.push({
          key: new Date(stat.timeStamp),
          data: stat.objAcceptRate,
        });
      }
      if(true === onConversion) {
        series[1].data.push({
          key: new Date(stat.timeStamp),
          data: stat.objConvRate,
        });
      }
      if(true === onProcess) {
        series[2].data.push({
          key: new Date(stat.timeStamp),
          data: stat.objProcRate,
        });
      }
      if(true === onDetected) {
        series[3].data.push({
          key: new Date(stat.timeStamp),
          data: stat.objFramesDetected,
        });
      }
      if(true === onConversion)
        channelCharts.objectCost.data[0].data = stat.objConvCost;
      if(true === onProcess)
        channelCharts.objectCost.data[1].data = stat.objProcCost;

      // publish
      series = channelCharts.publish.data;
      if(true === onAccept) {
        series[0].data.push({
          key: new Date(stat.timeStamp),
          data: stat.objPubAcceptRate,
        });
      }
      if(true === onProcess) {
        series[1].data.push({
          key: new Date(stat.timeStamp),
          data: stat.objPubProcRate,
        });
      }
      if(true === onProcess)
        channelCharts.objPubCost.data[0].data = stat.objPubProcCost;
    });

    return channelCharts;
  }

  function toggleLegendKey(event) {
    let series = event.target.outerText;
    if("accept" === series)
      setOnAccept(!onAccept);
    else if("conversion" === series)
      setOnConversion(!onConversion);
    else if("process" === series)
      setOnProcess(!onProcess);
    else if("detected" === series)
      setOnDetected(!onDetected);
  }

  let chartsByChannel = createCharts();
  let channelCharts = {};
  chartsByChannel.forEach((value, channel) => {
    channelCharts[channel] = {};
    let chartKeys = ["motion", "object", "publish"];
    chartKeys.forEach(key => {
      let chartData = chartsByChannel.get(channel)[key];
      let legendEntries = [];
      Object.keys(chartData.colors).forEach(k => legendEntries.push(
        <DiscreteLegendEntry label={k} color={chartData.colors[k]} onClick={toggleLegendKey}/>
      ));
      channelCharts[channel][key] = (
        <span key={channel + "-" + key} style={{display: "table-cell", padding: "3px"}}>
          <LineChart key="chart" width={300} height={100} data={chartData.data} series={
            <LineSeries
              type="grouped" animated={false} interpolation="smooth"
              colorScheme={(_data, index) => {
                if(_data.length > 0)
                  return chartData.colors[_data[0].key];
                else if(_data.key)
                  return chartData.colors[_data.key];
                return chartData.colors["accept"]
              }}
              line={<Line style={{opacity: "0.75"}}/>}
            />
          }
          />
          <DiscreteLegend key="legend" orientation="horizontal" entries={legendEntries}/>
        </span>
      );
    });

    // linear bar
    chartKeys = ["motionCost", "objectCost", "objPubCost"];
    chartKeys.forEach(key => {
      let chartData = chartsByChannel.get(channel)[key];
      channelCharts[channel][key] = (
        <span key={channel + "-" + key} style={{display: "table-cell", padding: "3px"}}>
          <BarChart width={100} height={100} data={chartData.data}
                    margins={15}
                    gridlines={null}
                    yAxis={<LinearYAxis axisLine={null}
                    />}
                    series={
                      <BarSeries
                        animated={true}
                        colorScheme={(_data, index) => {
                          return chartData.colors[_data.key];
                        }}
                        bar={<Bar gradient={null} width={7}
                                  padding={0}
                                  label={<BarLabel/>}
                        />}
                      />
                    }
          />
        </span>
      );
    });

  });

  let chartGroups = [];
  chartsByChannel.forEach((value, channel) => {
    let charts = [
      <label key="key"
             style={{display: "table-cell", writingMode: "tb", transform: "rotate(180deg)", textAlign: "center"}}>{channel}</label>
    ];
    charts.push(channelCharts[channel].motion);
    charts.push(channelCharts[channel].motionCost);
    charts.push(channelCharts[channel].object);
    charts.push(channelCharts[channel].objectCost);
    charts.push(channelCharts[channel].publish);
    charts.push(channelCharts[channel].objPubCost);
    let chartGroup = (
      <div key={channel} style={{display: "table-row-group"}}>
        {charts}
      </div>
    );
    chartGroups.push(chartGroup);
  });

  // if(chartGroups.length > 0) {
  //   chartGroups.push(
  //     <div key="legend" style={{display: "table-row-group"}}>
  //       <label key="blank" style={{display: "table-cell"}}></label>
  //       <label key="mot" style={{color: "green", display: "table-cell", textAlign: "center"}}>motion detection</label>
  //       <label key="motCost" style={{color: "green", display: "table-cell", textAlign: "center"}}>cost</label>
  //       <label key="obj" style={{color: "blue", display: "table-cell", textAlign: "center"}}>object detection</label>
  //       <label key="objCost" style={{color: "blue", display: "table-cell", textAlign: "center"}}>cost</label>
  //       <label key="pub" style={{color: "purple", display: "table-cell", textAlign: "center"}}>object publish RTSP</label>
  //       <label key="objPubCost" style={{color: "purple", display: "table-cell", textAlign: "center"}}>cost</label>
  //     </div>
  //   );
  // }

  return (
    <div style={{display: "table"}}>
      {chartGroups}
    </div>
  );

}

DashCharts.propTypes = {}

