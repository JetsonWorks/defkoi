import PropTypes from "prop-types";

// proper conversion and display of boolean values
// takes dataKey and formKey from colDef.field
export default class EntityBooleanCellEditorHelper {
  constructor(entityType, entityPatcher, cacheUpdater, cacheRefresher, options) {
    this.entityType = entityType;
    this.patcher = entityPatcher;
    this.cacheUpdater = cacheUpdater;
    this.cacheRefresher = cacheRefresher;

    this.includeEmptyOption = false;
    if(options) {
      ["includeEmptyOption"].forEach(key => {
        if(Object.keys(options).includes(key)) {
          this[key] = options[key];
        }
      });
    }

    this.valueGetter = this.valueGetter.bind(this);
    this.prepareUpdate = this.prepareUpdate.bind(this);
    this.valueSetter = this.valueSetter.bind(this);
    this.getCellEditorParams = this.getCellEditorParams.bind(this);
    this.valuePoster = this.valuePoster.bind(this);
  }

  // for passing the current values into cell editor
  valueGetter(params) {
    if(params.data[params.colDef.field] === null || params.data[params.colDef.field] === "")
      return null;
    return "true" === params.data[params.colDef.field] || true === params.data[params.colDef.field] ? "true" : "false";
  }

  // note: valueSetter() takes output from editor (array of select options) and updates data in the current row, but
  // since we're using Redux and the data is immutable, we need to dispatch an action, so
  // this prepares the action payload
  prepareUpdate(params) {
    let value = params.newValue;
    return {
      type: this.entityType,
      id: params.data.id,
      data: params.data,
      dataKey: params.colDef.field,
      formKey: params.colDef.field,
      value,
    };
  }

  // takes ValueSetterParams
  valueSetter(params) {
    if(params.data[params.colDef.field] === params.newValue) return;
    // update the Redux state
    let update = this.prepareUpdate(params);
    this.cacheUpdater(update);

    // in the meantime, replace this row data element with a clone with the updated value (for valuePoster)
    let temp = {...params.data};
    temp[update.dataKey] = update.value;
    params.node.updateData(temp);
  }

  // list of objects for select options
  getCellEditorParams() {
    let options = [];
    if(this.includeEmptyOption)
      options.push("");
    return {values: options.concat(["true", "false"])};
  }

  // the onCellValueChanged callback, takes CellValueChangedParams, delegates to entityPatcher
  valuePoster(params) {
    this.patcher(params.data, params.colDef.field,
        params.data[params.colDef.colId])
      .done(() => this.cacheRefresher({type: this.entityType}));
  }

}
EntityBooleanCellEditorHelper.propTypes = {
  entityType: PropTypes.string.isRequired,       // the type of entity being edited
  entityPatcher: PropTypes.func.isRequired,      // the BaseGrid patch function
  cacheUpdater: PropTypes.func.isRequired,       // the dispatch function to update the Redux cache
  cacheRefresher: PropTypes.func.isRequired,     // the dispatch function to query the cache item
  options: PropTypes.object,                     // modifiers, such as isCreatable
}
