// ref: https://www.ag-grid.com/react-grid/component-cell-editor/

import React, { Component } from "react";
import Select from "react-select";
import CreatableSelect from "react-select/creatable";

// wraps react-select and interfaces with ag-grid-react
// this class is used for scalar values
export default class SelectCellEditor extends Component {
  constructor(props) {
    super(props);
    this.inputRef = React.createRef();
    this.state = {
      currentValue: [],
    };
    if(Array.isArray(this.props.value))
      this.props.value.forEach(val => {
        this.state.currentValue.push({value: val, label: val});
      });
    else
      if(this.props.value)
        this.state.currentValue = {value: this.props.value, label: this.props.value};
      else
        this.state.currentValue = {value: null, label: null};

    this.includeEmptyOption = false;
    this.autoFocus = false;
    this.isSearchable = true;
    this.isMulti = false
    this.isCreatable = false;
    this.className = null;
  }

  isPopup() {
    return true;
  }

  // how ag-grid retrieves the selected values
  getValue() {
    return this.state.currentValue.value;
  }

// TODO: if current value is not an option, we get warning that each child should have unique "key" prop
  regenCurrentValue() {
    // Creatable.onChange() might have added a newly-created option, so check state.currentValue
    let selectedOption = [];
    if(this.state && this.state.currentValue != null)
      selectedOption = this.state.currentValue;
    // selectedOption is either the single option element or an array of options (in the case of multi-select)

    // selectedVals is an array containing just the option values
//    let selectedVals = [selectedOption.value];
//    if(Array.isArray(selectedOption))
//      selectedVals = selectedOption.map(function(op) {return op.value});
    return selectedOption;
  }

  render() {
    let currentValue = this.regenCurrentValue();
    const options = [];
    let maxLength = 1;

    if(true === this.includeEmptyOption)
      options.push({value: "", label: ""});
    this.props.values.forEach(val => {
      options.push({value: val, label: val});
      maxLength = Math.max(maxLength, val.length);
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

  rsChangeHandler(selectedOption) {
    this.setState({ currentValue: selectedOption });
  }

}
