import PropTypes from "prop-types";
import { EntityCellEditorHelper } from "../ag-grid"

// allows select options unique to the entity being edited
export default class CapCellEditorHelper extends EntityCellEditorHelper {
  constructor(entityType, objects, candidatesKey, entityPatcher, cacheUpdater, cacheRefresher, options) {
    super(entityType, objects, entityPatcher, cacheUpdater, cacheRefresher, options);
    this.candidatesKey = candidatesKey;
  }

  // list of objects for select options
  getCellEditorParams(params) {
    let options = [];
    if(!Array.isArray(params.data[this.candidatesKey]))
      throw new Error("Type " + this.entityType + " does not have an array under the name " + this.candidatesKey);
    options = params.data[this.candidatesKey]
      .filter(x => x.active === true || x.active === undefined)
      .sort((a, b) => {
          if(!a.formatted) return -1;
          if(!b.formatted) return 1;
          return a.formatted.localeCompare(b.formatted);
        });
    return {values: options};
  }

}
CapCellEditorHelper.propTypes = {
  entityType: PropTypes.string.isRequired,       // the type of entity being edited
  objects: PropTypes.array.isRequired,           // the array of objects to populate the select options
  entityPatcher: PropTypes.func.isRequired,      // the InfraGrid patch function
  cacheUpdater: PropTypes.func.isRequired,       // the dispatch function to update the Redux cache
  cacheRefresher: PropTypes.func.isRequired,       // the dispatch function to query the cache collection
  options: PropTypes.object,                     // modifiers, such as isCreatable
}

