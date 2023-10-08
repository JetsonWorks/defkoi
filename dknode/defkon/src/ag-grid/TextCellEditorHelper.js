import PropTypes from "prop-types";

// manages text field updates to entities
// takes dataKey and formKey from colDef.field (defaults to colDef.colId)
// dataKey (the name of the entity's property being edited)
//   defaults to colDef.colId
// formKey (the name of the request parameter)
//   defaults to colDef.field or colDef.colId
export default class TextCellEditorHelper {
  constructor(entityType, entityPatcher, cacheUpdater, cacheRefresher, options) {
    this.entityType = entityType;
    this.patcher = entityPatcher;
    this.cacheUpdater = cacheUpdater;
    this.cacheRefresher = cacheRefresher;

    if(options) {
      ["secondaryDataKey", "postValueChangesToParams"].forEach(key => {
        if(Object.keys(options).includes(key)) {
          this[key] = options[key];
        }
      });
    }

    this.valueGetter = this.valueGetter.bind(this);
    this.prepareUpdate = this.prepareUpdate.bind(this);
    this.valueSetter = this.valueSetter.bind(this);
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
    let val = params.data[params.colDef.colId];
    if(!val && this.secondaryDataKey)
      val = params.data[this.secondaryDataKey];
    return (!val || val === "") ? null : val;
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

  // takes ValueSetterParams
  valueSetter(params) {
    if(!params.oldValue && !params.newValue) return;
    if(params.oldValue === params.newValue) return;
    // update the Redux state
    let update = this.prepareUpdate(params);
    this.cacheUpdater(update);

    // in the meantime, replace this row data element with a clone with the updated value (for valuePoster)
    let temp = {...params.data};
    temp[update.dataKey] = update.value;
    params.node.updateData(temp);
  }

  // the onCellValueChanged callback, takes CellValueChangedParams, delegates to entityPatcher
  valuePoster(params) {
    if(!params.oldValue && !params.data[this.dataKey]) return;
    if(params.oldValue === params.data[this.dataKey]) return;
    if(this.postValueChangesToParams && this.postValueChangesToParams === true) {
      let edited = {...params.node.data};
      edited[this.dataKey] = params.data[this.dataKey];
      params.node.data = edited;
    }
    this.patcher(params.data, this.formKey,
        params.data[this.dataKey])
      .done(() => this.cacheRefresher({type: this.entityType}));
  }

}
TextCellEditorHelper.propTypes = {
  entityType: PropTypes.string.isRequired,       // the type of entity being edited
  entityPatcher: PropTypes.func.isRequired,      // the patch function
  cacheUpdater: PropTypes.func.isRequired,       // the dispatch function to update the Redux cache
  cacheRefresher: PropTypes.func.isRequired,     // the dispatch function to query the cache item
}
