// for custom edit, maybe extend PopupSelectCellEditor and override init()
// ref: https://www.ag-grid.com/react-grid/component-cell-editor/

import React from "react";
import Select from "react-select";
import CreatableSelect from "react-select/creatable";
import SelectCellEditor from "./SelectCellEditor";

// wraps react-select and interfaces with ag-grid-react
// selector of entity objects (e.g. entity is a pipeConf and nested entity is a device)
// labelKey (the nested property's key for the display value)
//   defaults to nested dot-property of colDef.colId (e.g. "displayName" from device.displayName)
// valueKey (the nested property's key for the request parameter value(s))
//   defaults to nested dot-property of colDef.field (e.g. "id" from deviceId.id)
export default class EntitySelectCellEditor extends SelectCellEditor {
  constructor(props) {
    super(props);
    if(!this.props.colDef.colId)
      throw new Error("Error: must set colId" + (this.props.colDef.field ? " for field " + this.props.colDef.field : ""));
    let pcs = this.props.colDef.colId.split(".");
    this.labelKey = pcs[1];
    if(!this.props.colDef.field)
      throw new Error("Error: must set field" + (this.props.colDef.colId ? " for colId " + this.props.colDef.colId : ""));
    pcs = this.props.colDef.field.split(".");
    this.valueKey = pcs[1];

    this.labelFormatter = entity => String(entity[this.labelKey]);
    this.includeEmptyOption = false;
    this.autoFocus = false;
    this.isSearchable = true;
    this.isMulti = false;
    this.isCreatable = false;
    this.className = null;
    this.currentValue = [];
    this.init = this.init.bind(this);
    this.init();
  }

  init(options) {
    if(options) {
      ["labelKey", "valueKey", "labelFormatter", "includeEmptyOption", "autoFocus",
          "isSearchable", "isMulti", "isCreatable", "className"].forEach(key => {
        if(Object.keys(options).includes(key)) {
          this[key] = options[key];
        }
      });
      this.state.currentValue = [];
    }
    if(Array.isArray(this.props.value))
      this.props.value.forEach(val => {
        this.state.currentValue.push({value: val[this.valueKey], label: this.labelFormatter(val)});
      });
    else
      if(this.props.value)
        this.state.currentValue = {value: this.props.value[this.valueKey], label: this.labelFormatter(this.props.value)};
      else
        this.state.currentValue = {value: null, label: null};
  }

  // how ag-grid retrieves the selected values
  getValue() {
    return this.state.currentValue;
  }

  render() {
    let currentValue = this.regenCurrentValue();
    const options = [];
    let maxLength = 1;

    if(true === this.includeEmptyOption)
      options.push({value: "", label: ""});
    this.props.values.forEach(entity => {
      if(typeof entity !== "object")
        throw new Error("Non-object passed to EntitySelectCellEditor for rendering");
      if(entity.active === true || entity.active === undefined) {
        options.push({value: entity[this.valueKey], label: this.labelFormatter(entity)});
        maxLength = Math.max(maxLength, this.labelFormatter(entity).length);
      }
    });

    let customStyles = {
      input: (provided, state) => ({
        minWidth: maxLength/2 + "em"
      })
    }
    if(true === this.isCreatable)
      return (
        <CreatableSelect className={this.className} styles={customStyles} value={currentValue}
          options={options} autoFocus={true === this.autoFocus}
          isMulti={true === this.isMulti} isSearchable={!(false === this.isSearchable)}
          onChange={(selectedOption) => {this.rsChangeHandler(selectedOption)}} />
      );
    return (
      <Select className={this.className} styles={customStyles} value={currentValue}
        options={options} autoFocus={true === this.autoFocus}
        isMulti={true === this.isMulti} isSearchable={!(false === this.isSearchable)}
        onChange={(selectedOption) => {this.rsChangeHandler(selectedOption)}} />
    );
  }

}
