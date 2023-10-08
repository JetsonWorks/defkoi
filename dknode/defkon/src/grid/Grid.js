import React from "react";
import $ from "jquery";
import { toast } from "react-toastify";
import classNames from "classnames";
import PropTypes from "prop-types";

import { connect } from "react-redux";
import { bindActionCreators } from 'redux';

import { restUrl } from "../App";
import { toastMessage } from "../AjaxResponse";
import "./styles.css";

import { AgGridReact } from 'ag-grid-react';

import { areCachesLoaded, checkCacheItems, entitiesNotLoaded, } from "../features/cache/CacheSlice";

import { addIcon, arrowRightIcon, arrowUpIcon, editIcon, multiCloneIcon, refreshIcon, retiredIcon, trashIcon, } from '../media';
import {
  EntityMultiSelectCellEditor,
  EntityMultiSelectCellEditorCreatable,
  EntitySelectCellEditor,
  EntitySelectCellEditorCreatable,
  EntitySelectCellEditorCreatableEmpty,
  EntitySelectCellEditorEmpty,
  SelectCellEditor,
  SelectCellEditorEmpty,
  ToggleCellEditor,
  ToggleCellEditorHelper,
} from "../ag-grid";

class Grid extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      loaded: false,
    };

    this.colDefs = [];

    this.onGridReady = this.onGridReady.bind(this);
    this.loadReferences = this.loadReferences.bind(this);
    this.renderTools = this.renderTools.bind(this);
    this.renderCacheStats = this.renderCacheStats.bind(this);
  }

  componentDidMount() {
    this.setState({
      cache: this.props.cache,
      prefs: this.props.prefs,
      actions: this.props.actions,
    });
    if(typeof this.props.collectionsRefresher === "function")
      this.props.collectionsRefresher();
    this.refEntityIds = {
      baseRestEntity: null,
    };
  }

  componentDidUpdate(prevProps, prevState, snapshot) {
    // cacheUsage is for referenced entity types and does not include the current Grid's entity type.
    // Besides checking cacheUsage, we also need to check for changes to the cache for the Grid's entity type.
    let deps = Array.from(this.props.cacheUsage);
    deps.push(this.props.itemName);
    if(deps.some(key => {
        let cache = this.props.cache[key];
        let prevCache = prevProps.cache[key];
        let prefs = this.props.prefs;
        let prevPrefs = prevProps.prefs;
        return (cache.status && cache.modTime && cache.status !== prevCache.status) || (cache.modTime !== prevCache.modTime)
          || (cache.touchTime !== prevCache.touchTime) || (prefs.length !== prevPrefs.length);
        })) {
      this.setState({
        cache: this.props.cache,
        prefs: this.props.prefs,
      });
      this.colDefs = [];
    } else {
      if(!this.state.loaded) {
        let refsLoaded = this.areReferencesLoaded("baseRestEntity");
        let loaded = this.areCachesLoaded() && refsLoaded;
        if(loaded) {
          this.setState({loaded: true});
          this.colDefs = [];
          // console.debug("Caches are loaded");
        }
        if(!refsLoaded) {
          return this.renderCacheStats();
        }
      }
    }
  }

  // if entity is retired, returns the "retired" class name
  getRowClass(params) {
    if(params.data.active === false) return "retired";
  };

  onGridReady(params) {
    if(typeof this.props.onGridReady === "function")
      this.props.onGridReady(params);
    else {
      this.setGridApi(params.api);
      this.setGridColumnApi(params.columnApi);
    }
  }

  setGridApi(gridApi) {
    this.setState({gridApi});
    this.colDefs = [];
  }

  setGridColumnApi(gridColumnApi) {
    this.setState({gridColumnApi});
  }

  onFirstDataRendered(params) {
    params.columnApi.autoSizeAllColumns();
  }

  // returns filtered set of entity references and requests any unloaded entities to be loaded
  loadReferences(entityType, refIds) {
    let neededRefEntityIds = [];
    if(refIds && refIds.length > 0) {
      if(refIds.filter(x => !x).length > 0) {
        console.warn("Warning: one or more referenced entity IDs are undefined (cache likely has entities without IDs) - Did you add your entity to ExposeEntityIdRestConfiguration.java?");
        refIds = refIds.filter(x => x);
      }
      this.refEntityIds[entityType] = refIds;

      neededRefEntityIds = entitiesNotLoaded(entityType, refIds);
      if(neededRefEntityIds.length > 0) {
        console.debug("Prioritizing " + neededRefEntityIds.length + " referenced entities");
        // split IDs into multiple requests
        for(let i=0; i<neededRefEntityIds.length; i+=500) {
          let end=Math.min(i+500, neededRefEntityIds.length);
          this.props.actions.checkCacheItems({type: entityType, entityIds: neededRefEntityIds.slice(i, end)});
        }
      }
    }
    return refIds;
  }

  // returns whether all referenced entities of the type (baseRestEntity) are loaded
  areReferencesLoaded(entityType) {
    if(!this.refEntityIds) return false;
    if(this.props.mdUsage) {
      for(let used of this.props.mdUsage) {
        if(!used || (Array.isArray(used) && used.length === 0)) {
          console.debug("Waiting on an element of mdUsage to be loaded");
          return false;
        }
      }
    }
    let neededRefEntityIds = [];
    let refIds = this.refEntityIds[entityType];
    if(refIds && refIds.length > 0)
      neededRefEntityIds = entitiesNotLoaded(entityType, refIds);
    return neededRefEntityIds.length === 0;
  }

  // returns whether dependent entity caches (but not the cache for this entity type) are loaded
  areCachesLoaded() {
    if(!this.state.cache) return false;
    for(let type of this.props.cacheUsage) {
      if(!this.state.cache[type]) return false;
      if(!this.state.cache[type].touchTime) {
        // console.debug("Cache " + type + " not initialized");
        return false;
      }
    }
    return areCachesLoaded(this.state.cache, this.props.cacheUsage);
  }

  renderTools() {
    if(typeof this.props.toolsBuilder !== "function")
      return [];
    let tools = this.props.toolsBuilder(this.state.gridApi);
    if(tools.length === 0)
      return (
        <label style={{marginTop: "1em"}}>&nbsp;</label>
      );
    return tools;
  }

  renderCacheStats() {
    if(!this.state.cache) {
      return null;
    }

    return (
      <div>
        {!this.state.cache.noCache.includes(this.props.itemName) &&
          <div>{this.state.cache[this.props.itemName].status}</div>
        }
        <div>{this.state.cache.status}</div>
      </div>
    );
  }

  render() {
    if(!this.state.loaded) {
      if(!this.areReferencesLoaded("baseRestEntity"))
        return this.renderCacheStats();
    }
    if(this.colDefs.length === 0 && true === this.props.prefs.loaded) {
      this.colDefs = this.props.buildColDefs();
      // set editable = false until caches are loaded
      if(!this.state.loaded)
        this.colDefs.forEach(x => x.editable = false);
      if(this.state.gridApi?.gridBodyCtrl?.isAlive() && this.state.gridApi.setColumnDefs) {
        this.state.gridApi.setColumnDefs(this.colDefs);
        setTimeout(() => this.state.gridApi.refreshHeader(), 100);
        setTimeout(() => this.state.gridApi.refreshCells({force: true}), 100);
      }
    }

    return (
      <div>
        {this.renderTools()}
        {!this.state.loaded && this.renderCacheStats()}
        <div id={this.props.title} className="ag-theme-moser" style={{height: this.props.gridHeight ? this.props.gridHeight : ""}}>
          <AgGridReact
            onGridReady={this.onGridReady}
            rowData={this.props.entities}
            gridOptions={this.props.gridOptions ? this.props.gridOptions : defaultGridOptions}
            defaultColDef={this.props.defaultColDef ? this.props.defaultColDef : defaultColDef}
            firstDataRendered={this.onFirstDataRendered}
            getRowClass={this.getRowClass}
            domLayout={this.props.gridHeight ? "normal" : "autoHeight"}
            components={this.props.frameworkComponents ? this.props.frameworkComponents : defaultFrameworkComponents}
            immutable={true}
            getRowId={data => data.data.id}
            animateRows={this.props.prefs.gridAnimatedRows}
            columnDefs={this.colDefs}
            >
          </AgGridReact>
        </div>
      </div>
    );
  }

}

export function renderAddButton(action) {
  return (
    <label key="add" style={{marginLeft: "7px", marginRight: "1em"}}>
      <input id="add" type="image" src={addIcon} alt="add" width="15" value="add" onClick={action} />
    </label>
  );
}

export function add(itemName, suggName) {
  let url = restUrl + itemName + "/add";
  let params = {};
  if(suggName)
    params.name = suggName;
  return $.ajax({method: "POST", data: params, url: url})
  .done((data, textStatus, jqXHR) => {
    toast.success("Added", { position: "bottom-right" });
  })
  .fail((jqXHR, textStatus, errorThrown) => {
    toastMessage(jqXHR);
  });
}

export function renderMultiEditButton(action) {
  return (
    <label key="multiEdit" style={{marginLeft: "7px", marginRight: "1em"}}>
      <input id="multiEdit" type="image" src={editIcon} alt="multi-edit" width="15" value="multiEdit" onClick={action} />
    </label>
  );
}

export function multiEdit(selectedItemsGetter, modal) {
  let items = selectedItemsGetter();
  if(items.length === 0)
    return;
  modal.current.refresh(items);
}

export function renderMultiDeleteButton(action) {
  return (
    <label key="multiDelete" style={{marginLeft: "7px", marginRight: "1em"}}>
      <input id="multiDelete" type="image" src={trashIcon} alt="multi-delete" width="15" value="multiDelete" onClick={action} />
    </label>
  );
}

export function multiDelete(selectedItemsGetter, itemName, entityRemover) {
  let items = selectedItemsGetter();
  if(items.length === 0)
    return;
  let promises = [];
  items.forEach(item => {
    let url = restUrl + itemName + "/delete/" + item.id;
    entityRemover({type: itemName, id: item.id});
    promises.push($.ajax({method: "POST", url: url})
    .done((data, textStatus, jqXHR) => {
      toast.success("Deleted", { position: "bottom-right" });
    })
    .fail((jqXHR, textStatus, errorThrown) => {
      toastMessage(jqXHR);
    }));
  });
  return Promise.allSettled(promises);
}

export function renderCloneButton(action) {
  return (
    <label key="multiClone" style={{marginLeft: "7px", marginRight: "1em"}}>
      <input id="multiClone" type="image" src={multiCloneIcon} alt="Clone selected entities" width="15" value="multiClone" onClick={action} />
    </label>
  );
}

export function multiClone(selectedItemsGetter, modal) {
  let items = selectedItemsGetter();
  if(items.length === 0)
    return;
  modal.current.refresh(items);
}

export function renderShowRetiredButton(prefs, showRetiredPrefKey, action) {
  let cnames = classNames({ 'optionInactive': !prefs[showRetiredPrefKey] });
  return (
    <label className={cnames} key="showRetired" style={{marginLeft: "7px", marginRight: "1em"}}>
      <input id="showRetired" type="image" src={retiredIcon} alt="Show/Hide retired entities" width="15" value="showRetired" onClick={action} />
    </label>
  );
}

export function setShowHideRetired(prefs, showRetiredPrefKey, prefSetter, prefsSaver) {
  let newVal = !prefs[showRetiredPrefKey];
  prefSetter({key: showRetiredPrefKey, value: newVal});
  let p = {...prefs};
  p[showRetiredPrefKey] = newVal;
  prefsSaver(p);
}

export function renderExportButton(action) {
  return (
    <label key="export" style={{marginLeft: "7px", marginRight: "1em"}}>
      <input id="export" type="image" src={arrowRightIcon} alt="export" width="15" value="export" onClick={action} />
    </label>
  );
}

export function renderReinitButton(action) {
  return (
      <label key="reinit" style={{marginLeft: "7px", marginRight: "1em"}}>
        <input id="reinit" type="image" src={refreshIcon} alt="reinit" width="15" value="reinit" onClick={action} />
      </label>
  );
}

export function renderActivateButton(action) {
  return (
    <label key="activate" style={{marginLeft: "7px", marginRight: "1em"}}>
      <input id="activate" type="image" src={arrowUpIcon} alt="activate" width="15" value="activate" onClick={action} />
    </label>
  );
}

export function getSelectedItems(gridApi) {
  let selected = (!gridApi || !gridApi.getSelectedNodes()) ? [] : gridApi.getSelectedNodes().map(row => row.data);
  return selected;
}

// including columnDefs in these grid options doesn't seem to work
export const defaultGridOptions = {
  pagination: true,
  paginationPageSize: 50,
  rowSelection: "multiple",
  editType: "fullRow",
}

// so column def is defined separately
export const defaultColDef = {
  width: 100,
  sortable: true,
  filter: true,
  resizable: true,
  editable: false,
  wrapText: true,
  autoHeight: true,
  enableCellChangeFlash: true,
}

export const defaultFrameworkComponents = {
  "selectCellEditor": SelectCellEditor,
  "selectCellEditorEmpty": SelectCellEditorEmpty,
  "entitySelectCellEditor": EntitySelectCellEditor,
  "entitySelectCellEditorEmpty": EntitySelectCellEditorEmpty,
  "entitySelectCellEditorCreatable": EntitySelectCellEditorCreatable,
  "entitySelectCellEditorCreatableEmpty": EntitySelectCellEditorCreatableEmpty,
  "entityMultiSelectCellEditor": EntityMultiSelectCellEditor,
  "entityMultiSelectCellEditorCreatable": EntityMultiSelectCellEditorCreatable,
  "toggleCellEditor": ToggleCellEditor,
  "toggleCellEditorHelper": ToggleCellEditorHelper,
};

// map to this.props from the store state
const mapStateToProps = (state) => ({
  prefs: state.metadata.prefs,
  cache: state.cache,
});

// bind our actions to this.props
const mapDispatchToProps = (dispatch) => ({
  actions: bindActionCreators({checkCacheItems}, dispatch)
});

// connect our component to the redux store
export default connect(
  mapStateToProps,
  mapDispatchToProps,
  null,
  { forwardRef: true } // must be supplied for react/redux when using AgGridReact
)(Grid);

Grid.propTypes = {
  entities: PropTypes.array.isRequired,             // array of entities
  gridOptions: PropTypes.object,                    // ag-grid grid-options object
  defaultColDef: PropTypes.object,                  // ag-grid defaultColDef object
  frameworkComponents: PropTypes.object,            // ag-grid frameworkComponents object
  cacheUsage: PropTypes.array.isRequired,           // array of entity names this data relies upon
  mdUsage: PropTypes.array,                         // array of metadata objects we depend upon (selected from metadata)
  collectionsRefresher: PropTypes.func,             // refreshes caches needed for this grid
  toolsBuilder: PropTypes.func,                     // builds the tools array, given gridApi
  mapRefBaseEntities: PropTypes.func,               // maps referenced BaseRestEntities
  buildColDefs: PropTypes.func.isRequired,          // builds column definitions
  onGridReady: PropTypes.func,                      // override function for capturing the gridApi
  gridHeight: PropTypes.string,                     // defaults to "autoHeight", or set a height in pixels (e.g. "500px")
}

