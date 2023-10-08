import React from 'react';
import PropTypes from "prop-types";

import { restUrl } from "../App";
import "./styles.css";
import "../defkoi.css";
import { EntityCellEditorHelper, TextCellEditorHelper, ToggleCellEditorHelper, } from "../ag-grid";
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
import { hasRoleOperator, selectFilterText, selectPrefs, setPref, } from '../features/metadata/MdSlice';
import { savePrefs } from '../view/Preferences.js';

import { refreshCache, removeEntity, selectCache, updateEditedEntity, } from "../features/cache/CacheSlice";

const GridLoader = React.forwardRef((props, ref) => {
  const cache = useSelector(selectCache);
  const dispatch = useDispatch();
  const isOperator = useSelector(hasRoleOperator);
  const prefs = useSelector(selectPrefs);
  const filterText = useSelector(selectFilterText);

  let searchFunc = null;
  let gridOptions = defaultGridOptions;
  if(props.pageSizeInterval)
    gridOptions.paginationPageSize = props.pageSizeInterval;
  let defColDef = defaultColDef;
  defColDef.editable = isOperator;

  const cacheUsage = ["device"];

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
          if(params.column.colId==="device")
            return params.node.data.device ? params.node.data.device.name : null;
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
    let deviceHelper = new EntityCellEditorHelper("capability", cache.device.entities,
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let nameHelper = new TextCellEditorHelper("capability",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let formatHelper = new TextCellEditorHelper("capability",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let widthHelper = new TextCellEditorHelper("capability",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let heightHelper = new TextCellEditorHelper("capability",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let arHelper = new TextCellEditorHelper("capability",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let frMinHelper = new TextCellEditorHelper("capability",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let frMaxHelper = new TextCellEditorHelper("capability",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);
    let activeHelper = new ToggleCellEditorHelper("capability",
      patchEntity, (entity) => dispatch(updateEditedEntity(entity)), refreshCollections);

    colDefs.push({colId: "device.displayName", field: "deviceId.id", editable: false, headerName: "Device", flex: 2,
      valueFormatter: deviceHelper.valueFormatter, valueGetter: deviceHelper.valueGetter, sort: 'asc', sortIndex: 0,
      valueSetter: deviceHelper.valueSetter, filterParams: deviceHelper.filterParams,
      cellEditor: "entitySelectCellEditor", cellEditorParams: deviceHelper.getCellEditorParams,
      onCellValueChanged: deviceHelper.valuePoster, comparator: deviceHelper.comparator});
    colDefs.push({colId: "name", field: "name", flex: 1, sort: 'desc', sortIndex: 1,
      valueGetter: nameHelper.valueGetter,
      valueSetter: nameHelper.valueSetter,
      onCellValueChanged: nameHelper.valuePoster});
    colDefs.push({colId: "format", field: "format", flex: 1, sort: 'asc', sortIndex: 2,
      valueGetter: formatHelper.valueGetter,
      valueSetter: formatHelper.valueSetter,
      onCellValueChanged: formatHelper.valuePoster});
    colDefs.push({colId: "width", field: "width", flex: 1, sort: 'asc', sortIndex: 4,
      valueGetter: widthHelper.valueGetter,
      valueSetter: widthHelper.valueSetter,
      onCellValueChanged: widthHelper.valuePoster});
    colDefs.push({colId: "height", field: "height", flex: 1,
      valueGetter: heightHelper.valueGetter,
      valueSetter: heightHelper.valueSetter,
      onCellValueChanged: heightHelper.valuePoster});
    colDefs.push({colId: "aspectRatio", field: "aspectRatio", flex: 1,
      valueGetter: arHelper.valueGetter,
      valueSetter: arHelper.valueSetter,
      onCellValueChanged: arHelper.valuePoster});
    colDefs.push({colId: "framerateMin", field: "framerateMin", flex: 1, sort: 'desc', sortIndex: 3,
      valueGetter: frMinHelper.valueGetter,
      valueSetter: frMinHelper.valueSetter,
      onCellValueChanged: frMinHelper.valuePoster});
    colDefs.push({colId: "framerateMax", field: "framerateMax", flex: 1,
      valueGetter: frMaxHelper.valueGetter,
      valueSetter: frMaxHelper.valueSetter,
      onCellValueChanged: frMaxHelper.valuePoster});

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
                collectionsRefresher={refreshCollections}
                toolsBuilder={toolsBuilder}
                mapRefBaseEntities={mapReferencedEntities}
                buildColDefs={buildColDefs}
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

