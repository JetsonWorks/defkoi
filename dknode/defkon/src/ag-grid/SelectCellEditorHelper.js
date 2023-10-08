import PropTypes from "prop-types";

// for select options of simple values (contrasted with EntityCellEditorHelper)
// takes dataKey and formKey from colDef.field (defaults to colDef.colId)
// dataKey (the name of the entity's property being edited)
//   defaults to colDef.colId
// formKey (the name of the request parameter)
//   defaults to colDef.field or colDef.colId
export default class SelectCellEditorHelper {
  constructor(entityType, objects, entityPatcher, cacheUpdater, cacheRefresher, options) {
    this.entityType = entityType;
    this.objects = objects;
    this.patcher = entityPatcher;
    this.cacheUpdater = cacheUpdater;
    this.cacheRefresher = cacheRefresher;

    this.includeEmptyOption = false;
    this.isCreatable = false;

    if(options) {
      ["includeEmptyOption, isCreatable"].forEach(key => {
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
    if(!this.dataKey) {
      if(!params.colDef.colId)
        throw new Error("Error: must set colId" + (params.colDef.field ? " for field " + params.colDef.field : ""));
      this.dataKey = params.colDef.colId;
    }
    if(!this.formKey) {
      this.formKey = params.colDef.field ? params.colDef.field : this.dataKey;
    }
    return params.data[this.dataKey];
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
      dataKey: this.dataKey,
      formKey: this.formKey,
      value,
    };
  }

  valueSetter(valueSetterParams) {
    // update the Redux state
    let update = this.prepareUpdate(valueSetterParams);
    this.cacheUpdater(update);

    // in the meantime, replace this row data element with a clone with the updated value (for valuePoster)
    let temp = {...valueSetterParams.data};
    temp[update.dataKey] = update.value;
    valueSetterParams.node.updateData(temp);
  }

  // list of objects for select options
  getCellEditorParams() {
    let options = [];
    if(this.includeEmptyOption)
      options.push("");
    return {values: options.concat(this.objects)};
  }

  // the onCellValueChanged callback, delegates to entityPatcher
  valuePoster(cellValueChangedParams) {
    this.patcher(cellValueChangedParams.data, this.formKey, cellValueChangedParams.data[this.dataKey])
      .done(() => this.cacheRefresher({type: this.entityType}));
  }

}
SelectCellEditorHelper.propTypes = {
  entityType: PropTypes.string.isRequired,       // the type of entity being edited
  objects: PropTypes.array.isRequired,           // the array of objects to populate the select options
  entityPatcher: PropTypes.func.isRequired,      // the BaseGrid patch function
  cacheUpdater: PropTypes.func.isRequired,       // the dispatch function to update the Redux cache
  cacheRefresher: PropTypes.func.isRequired,     // the dispatch function to query the cache item
  options: PropTypes.object,                     // modifiers, such as isCreatable
}

