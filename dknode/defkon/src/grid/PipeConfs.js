import React from 'react';
import PropTypes from "prop-types";

import { restUrl } from "../App";
import "./styles.css";
import "../defkoi.css";
import { CapCellEditorHelper, EntityCellEditorHelper, TextCellEditorHelper, ToggleCellEditorHelper, } from "../ag-grid";
import { entityPatcher, } from "./Utils";
import { makeRequest, } from "../Utils";

import { SearchAPI } from "../search/SearchPage";
import Grid, {
  add,
  defaultColDef,
  defaultFrameworkComponents,
  defaultGridOptions,
  getSelectedItems,
  multiDelete,
  renderActivateButton,
  renderAddButton,
  renderExportButton,
  renderMultiDeleteButton,
  renderReinitButton,
  renderShowRetiredButton,
  setShowHideRetired,
} from "./Grid";

import { useDispatch, useSelector } from 'react-redux';
import { hasRoleOperator, selectConfig, selectFilterText, selectPrefs, setPref, } from '../features/metadata/MdSlice';
import { savePrefs } from '../view/Preferences.js';

import { refreshCache, removeEntity, selectCache, updateEditedEntity, } from "../features/cache/CacheSlice";

const GridLoader = React.forwardRef((props, ref) => {
  const cache = useSelector(selectCache);
  const dispatch = useDispatch();
  const isOperator = useSelector(hasRoleOperator);
  const prefs = useSelector(selectPrefs);
  const config = useSelector(selectConfig);
  const filterText = useSelector(selectFilterText);

  let searchFunc = null;
  let gridOptions = defaultGridOptions;
  if(props.pageSizeInterval)
    gridOptions.paginationPageSize = props.pageSizeInterval;
  let defColDef = defaultColDef;
  defColDef.editable = isOperator;
  let fmwk = defaultFrameworkComponents;

  const cacheUsage = ["device", "capability"];

  function enabledMultiSelectTools() {
    return true === props.enableMultiSelectTools;
  }

  function toolsBuilder(gridApi) {
    let tools = [];

    tools.push(renderAddButton(
      () => {
        add(props.itemName, filterText);
        refreshCollections();
        setTimeout(searchFunc, 1000);
      }
    ));

    if(enabledMultiSelectTools()) {
      tools.push(renderMultiDeleteButton(
        () => multiDelete(
          () => getSelectedItems(gridApi),
          props.itemName,
          (params) => dispatch(removeEntity(params))
        )
      ));
    }

    tools.push(renderShowRetiredButton(prefs, props.showRetiredPrefKey,
      () => {
        setShowHideRetired(prefs, props.showRetiredPrefKey,
          (params) => dispatch(setPref(params)),
          (prefs) => savePrefs(prefs)
        );
        setTimeout(searchFunc, 1000);
      }
    ));

    tools.push(renderReinitButton(
      () => { makeRequest(restUrl + "reinit", null, "Reinitializing", "Reinitialized").then(refreshCollections); }
    ));

    tools.push(renderActivateButton(
      () => { makeRequest(restUrl + "activate", null, "Activating changes", "Activated changes").then(refreshCollections); }
    ));

    tools.push(renderExportButton(
      () => gridApi.exportDataAsCsv({
        // value, node, column, api, columnApi, context, type
        processCellCallback: (params) => {
          if(params.column.colId==="device.displayName")
            return params.node.data.device ? params.node.data.device.displayName : null;
          if(params.column.colId==="cap.displayName")
            return params.node.data.cap ? params.node.data.cap.formatted : null;
          return params.value;
        }
      })
    ));

    return tools;
  }

  function mapReferencedEntities(results) {
    let refEntityIds = [];
    results.forEach(row => {
      if(row.device) refEntityIds.push(row.device.id);
    });
    return refEntityIds;
  }

  function buildColDefs() {
    let colDefs = [];
    let enabledHelper = new ToggleCellEditorHelper("pipeConf",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let nameHelper = new TextCellEditorHelper("pipeConf",
        patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let nvEnabledHelper = new ToggleCellEditorHelper("pipeConf",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let motDetectHelper = new ToggleCellEditorHelper("pipeConf",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let objDetectHelper = new ToggleCellEditorHelper("pipeConf",
        patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let deviceHelper = new EntityCellEditorHelper("pipeConf", cache.device.entities,
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let capHelper = new CapCellEditorHelper("pipeConf", cache.capability.entities, "validCaps",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let activeHelper = new ToggleCellEditorHelper("pipeConf",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);

    colDefs.push({colId: "enabled", headerName: "Enabled", minWidth: 50,
      valueGetter: enabledHelper.valueGetter, valueSetter: enabledHelper.valueSetter,
      cellRenderer: "toggleCellEditor", cellEditor: "toggleCellEditor",
      onCellValueChanged: enabledHelper.valuePoster});
    colDefs.push({colId: "name", field: "name", minWidth: 140, sort: 'asc',
      valueGetter: nameHelper.valueGetter, valueSetter: nameHelper.valueSetter,
      onCellValueChanged: nameHelper.valuePoster});
    colDefs.push({colId: "nvEnabled", headerName: "NV-Enabled", minWidth: 50, editable: isOperator && true === config.nvCapable,
      valueGetter: nvEnabledHelper.valueGetter, valueSetter: nvEnabledHelper.valueSetter,
      cellRenderer: "toggleCellEditor", cellEditor: "toggleCellEditor",
      onCellValueChanged: nvEnabledHelper.valuePoster});
    colDefs.push({colId: "motDetectEnabled", headerName: "Mot. Detect", minWidth: 50, editable: isOperator && true === config.motDetectEnabled,
      valueGetter: motDetectHelper.valueGetter, valueSetter: motDetectHelper.valueSetter,
      cellRenderer: "toggleCellEditor", cellEditor: "toggleCellEditor",
      onCellValueChanged: motDetectHelper.valuePoster});
    colDefs.push({colId: "objDetectEnabled", headerName: "Obj. Detect", minWidth: 50, editable: isOperator && true === config.objDetectEnabled,
      valueGetter: objDetectHelper.valueGetter, valueSetter: objDetectHelper.valueSetter,
      cellRenderer: "toggleCellEditor", cellEditor: "toggleCellEditor",
      onCellValueChanged: objDetectHelper.valuePoster});
    colDefs.push({colId: "device.displayName", field: "deviceId.id", headerName: "Device", flex: 2,
      valueFormatter: deviceHelper.valueFormatter, valueGetter: deviceHelper.valueGetter,
      valueSetter: deviceHelper.valueSetter, filterParams: deviceHelper.filterParams,
      cellEditor: "entitySelectCellEditor", cellEditorParams: deviceHelper.getCellEditorParams,
      onCellValueChanged: deviceHelper.valuePoster, comparator: deviceHelper.comparator});
    colDefs.push({colId: "cap.formatted", field: "capId.id", headerName: "Device Caps", flex: 2,
      valueFormatter: capHelper.valueFormatter, valueGetter: capHelper.valueGetter,
      valueSetter: capHelper.valueSetter, filterParams: capHelper.filterParams,
      cellEditor: "entitySelectCellEditorEmpty", cellEditorParams: capHelper.getCellEditorParams,
      onCellValueChanged: capHelper.valuePoster, comparator: capHelper.comparator});

    colDefs.push({colId: "modified", editable: false, headerName: "Modified", sortingOrder: ['desc','asc',null], minWidth: 140,
      valueGetter: params => params.data.modTime ? params.data.modTime.replace(/\..*/, "").replace(/T/, " ") : null});
    if(prefs[props.showRetiredPrefKey])
      colDefs.push({colId: "active", field: "setActive", minWidth: 50,
        valueGetter: activeHelper.valueGetter, valueSetter: activeHelper.valueSetter,
        cellRenderer: "toggleCellEditor", cellEditor: "toggleCellEditor",
        onCellValueChanged: activeHelper.valuePoster});
    return colDefs;
  }

  function refreshCollections() {
    dispatch(refreshCache({type: props.itemName}));
    cacheUsage.forEach(type => dispatch(refreshCache({type: type})));
  }

  function patchEntity(data, formKey, value) {
    return entityPatcher(props.itemName, data, formKey, value);
  }

  return (
    <div>
      <SearchAPI.Consumer>
        {context => {
          if(!context.results) {
            throw new Error("Error: context not properly initialized");
          }
          searchFunc = context.search;
          let entities = props.entities ? props.entities : context.results;

          return (
            <div className="ag-theme-moser">
              <Grid ref={ref}
                entities={entities}
                gridOptions={gridOptions}
                defaultColDef={defColDef}
                frameworkComponents={fmwk}
                cacheUsage={cacheUsage}
                collectionsRefresher={refreshCollections}
                toolsBuilder={toolsBuilder}
                mapRefBaseEntities={mapReferencedEntities}
                buildColDefs={buildColDefs}
                gridHeight="400px"
                {...props} />
            </div>
          );
        }}
      </SearchAPI.Consumer>
    </div>
  );

});
export default GridLoader;

Grid.propTypes = {
  entities: PropTypes.array.isRequired,             // array of entities
  itemName: PropTypes.string.isRequired,            // the entity type name
  showRetiredPrefKey: PropTypes.string.isRequired,  // the key under which the entity-specific show retired pref is saved
  pageSizeInterval: PropTypes.number,               // size of each page (default 50)
  enableMultiSelectTools: PropTypes.bool,           // whether multi-select tools should be enabled (default false)
}

