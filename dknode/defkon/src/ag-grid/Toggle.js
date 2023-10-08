import React, { forwardRef, useEffect, useImperativeHandle, useRef, useState, } from "react";
import PropTypes from "prop-types";
import "../toggle-button.css";

// takes value from colDef.colId
export const ToggleCellEditor = forwardRef((props, ref) => {
  const [value, setValue] = useState(props.data[props.colDef.colId]);
  const refInput = useRef(null);

  useEffect(() => {
    refInput.current.focus();
  }, []);

  /* Component Editor Lifecycle methods */
  useImperativeHandle(ref, () => {
    return {
      // the final value to send to the grid, on completion of editing
      getValue() { return value; },
      isCancelBeforeStart() { return false; },
      isCancelAfterEnd() { return false; },
    };
  });

  let editable = typeof props.colDef.editable === "function" ? props.colDef.editable(props) : props.colDef.editable;
  return (
    <input type="checkbox" ref={refInput} className="cm-toggle green" disabled={false === editable}
      checked={value} style={{marginTop: "0px"}}
      onChange={event => {
        setValue(true === event.target.checked);
        if(props.setValue) {
          props.setValue(true === event.target.checked);
        }
      }}
    />
  );
});

// renders a checkbox styled like a toggle switch
// keys entities by ID (for caching) and takes other parameters from colDef.colId and colDef.field
// dataKey (the name of the entity's property being edited)
//   defaults to colDef.colId
// formKey (the name of the request parameter)
//   defaults to colDef.field
export class ToggleCellEditorHelper {
  constructor(entityType, entityPatcher, cacheUpdater, cacheRefresher, options) {
    this.entityType = entityType;
    this.patcher = entityPatcher;
    this.cacheUpdater = cacheUpdater;
    this.cacheRefresher = cacheRefresher;

    if(options) {
      ["dataKey", "formKey"].forEach(key => {
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
    if(params.data[this.dataKey] === null || params.data[this.dataKey] === "")
      return null;
    return "true" === params.data[this.dataKey] || true === params.data[this.dataKey] ? "true" : "false";
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
    if(params.data[this.dataKey] === params.newValue) return;
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
    let oldValue = params.oldValue === "true" || params.oldValue === true;
    if(oldValue === params.data[this.dataKey]) return;
    this.patcher(params.data, this.formKey,
        params.data[this.dataKey])
      .done(() => this.cacheRefresher({type: this.entityType}));
  }

}
ToggleCellEditorHelper.propTypes = {
  entityType: PropTypes.string.isRequired,       // the type of entity being edited
  entityPatcher: PropTypes.func.isRequired,      // the BaseGrid patch function
  cacheUpdater: PropTypes.func.isRequired,       // the dispatch function to update the Redux cache
  cacheRefresher: PropTypes.func.isRequired,     // the dispatch function to query the cache item
  options: PropTypes.object,                     // modifiers, such as isCreatable
  className: PropTypes.string,                   // optional class to apply
}
