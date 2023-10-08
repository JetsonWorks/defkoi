import PropTypes from "prop-types";
import { EntityCellEditorHelper } from "../ag-grid"

// Read-only doubly-nested: third argument is the nested key
export default class NestedEntityCellEditorHelper extends EntityCellEditorHelper {
  constructor(entityType, objects, nestedKey, entityPatcher, cacheUpdater, cacheRefresher, options) {
    super(entityType, objects, entityPatcher, cacheUpdater, cacheRefresher, options);
    this.nestedKey = nestedKey;
  }

  getValue(entity) {
    if(!entity[this.dataKey]) return null;
    let ids = entity[this.dataKey].map(x => x.id);
    return this.objects.filter(x => ids.includes(x.id)).map(x => x[this.nestedKey]);
  }

}
NestedEntityCellEditorHelper.propTypes = {
  entityType: PropTypes.string.isRequired,       // the type of entity being edited
  objects: PropTypes.array.isRequired,           // the array of objects to populate the select options
  entityPatcher: PropTypes.func.isRequired,      // the InfraGrid patch function
  cacheUpdater: PropTypes.func.isRequired,       // the dispatch function to update the Redux cache
  cacheRefresher: PropTypes.func.isRequired,     // the dispatch function to query the cache collection
  options: PropTypes.object,                     // modifiers, such as isCreatable
}

