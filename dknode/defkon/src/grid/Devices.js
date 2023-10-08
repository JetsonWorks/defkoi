import React from 'react';
import PropTypes from "prop-types";

import { restUrl } from "../App";
import "./styles.css";
import "../defkoi.css";
import { CapCellEditorHelper, SelectCellEditorHelper, TextCellEditorHelper, ToggleCellEditorHelper, } from "../ag-grid";
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
import { hasRoleOperator, selectDeviceApis, selectFilterText, selectPrefs, setPref, } from '../features/metadata/MdSlice';
import { savePrefs } from '../view/Preferences.js';

import { refreshCache, removeEntity, selectCache, updateEditedEntity, } from "../features/cache/CacheSlice";

const GridLoader = React.forwardRef((props, ref) => {
  const cache = useSelector(selectCache);
  const dispatch = useDispatch();
  const isOperator = useSelector(hasRoleOperator);
  const prefs = useSelector(selectPrefs);
  const deviceApis = useSelector(selectDeviceApis);
  const filterText = useSelector(selectFilterText);

  let searchFunc = null;
  let gridOptions = defaultGridOptions;
  if(props.pageSizeInterval)
    gridOptions.paginationPageSize = props.pageSizeInterval;
  gridOptions.editType = "fullRow";
  let defColDef = defaultColDef;
  defColDef.editable = isOperator;

  const cacheUsage = ["capability"];

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
          if(params.column.colId==="capabilities")
            return params.node.data.caps.map(x => x.formatted).join("\n");
          return params.value;
        }
      })
    ));

    return tools;
  }

  function mapReferencedEntities(results) {
    let refEntityIds = [];
    results.forEach(row => {
      if(row.caps) refEntityIds = refEntityIds.concat(row.caps.map(x => x.id));
    });
    return refEntityIds;
  }

  function buildColDefs() {
    let colDefs = [];
    let enabledHelper = new ToggleCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let dCardHelper = new TextCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let busInfoHelper = new TextCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let nameHelper = new TextCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let dNameHelper = new TextCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let dClassHelper = new TextCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let dPathHelper = new TextCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let dApiHelper = new SelectCellEditorHelper("device", deviceApis,
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let dDriverHelper = new TextCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let capsHelper = new CapCellEditorHelper("device", cache.capability.entities, "caps",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let activeHelper = new ToggleCellEditorHelper("device",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);

    colDefs.push({colId: "enabled", headerName: "Enabled", minWidth: 50,
      valueGetter: enabledHelper.valueGetter,
      valueSetter: enabledHelper.valueSetter,
      cellRenderer: "toggleCellEditor",
      cellEditor: "toggleCellEditor",
      onCellValueChanged: enabledHelper.valuePoster});
    colDefs.push({colId: "deviceCard", field: "deviceCard", flex: 1,
      valueGetter: dCardHelper.valueGetter,
      valueSetter: dCardHelper.valueSetter,
      onCellValueChanged: dCardHelper.valuePoster});
    colDefs.push({colId: "deviceBusInfo", field: "deviceBusInfo", flex: 1,
      valueGetter: busInfoHelper.valueGetter,
      valueSetter: busInfoHelper.valueSetter,
      onCellValueChanged: busInfoHelper.valuePoster});
    colDefs.push({colId: "name", field: "name", minWidth: 140,
      valueGetter: nameHelper.valueGetter,
      valueSetter: nameHelper.valueSetter,
      onCellValueChanged: nameHelper.valuePoster});
    colDefs.push({colId: "displayName", field: "displayName", flex: 1,
      valueGetter: dNameHelper.valueGetter,
      valueSetter: dNameHelper.valueSetter,
      onCellValueChanged: dNameHelper.valuePoster});
    colDefs.push({colId: "deviceClass", field: "deviceClass", flex: 1,
      valueGetter: dClassHelper.valueGetter,
      valueSetter: dClassHelper.valueSetter,
      onCellValueChanged: dClassHelper.valuePoster});
    colDefs.push({colId: "devicePath", field: "devicePath", flex: 1,
      valueGetter: dPathHelper.valueGetter,
      valueSetter: dPathHelper.valueSetter,
      onCellValueChanged: dPathHelper.valuePoster});
    colDefs.push({colId: "deviceApi", field: "deviceApi", flex: 1,
      valueFormatter: dApiHelper.valueFormatter, valueGetter: dApiHelper.valueGetter,
      valueSetter: dApiHelper.valueSetter,
      cellEditor: "selectCellEditor", cellEditorParams: dApiHelper.getCellEditorParams,
      onCellValueChanged: dApiHelper.valuePoster});
    colDefs.push({colId: "deviceDriver", field: "deviceDriver", flex: 1,
      valueGetter: dDriverHelper.valueGetter,
      valueSetter: dDriverHelper.valueSetter,
      onCellValueChanged: dDriverHelper.valuePoster});
    colDefs.push({colId: "caps.formatted", field: "capId.id", headerName: "Capabilities", width: 250, editable: false,
      valueFormatter: capsHelper.valueFormatter, valueGetter: capsHelper.valueGetter,
      valueSetter: capsHelper.valueSetter, filterParams: capsHelper.filterParams,
      cellEditor: "entityMultiSelectCellEditor", cellEditorParams: capsHelper.getCellEditorParams,
      onCellValueChanged: capsHelper.valuePoster, comparator: capsHelper.comparator});

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
              <Grid
                entities={entities}
                gridOptions={gridOptions}
                defaultColDef={defColDef}
                frameworkComponents={defaultFrameworkComponents}
                cacheUsage={cacheUsage}
                mdUsage={[deviceApis]}
                collectionsRefresher={refreshCollections}
                toolsBuilder={toolsBuilder}
                mapRefBaseEntities={mapReferencedEntities}
                buildColDefs={buildColDefs}
                gridHeight="750px"
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

