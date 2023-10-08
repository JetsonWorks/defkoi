import React, { Component } from "react";
import PropTypes from "prop-types";
import Select from "react-select";
import CreatableSelect from "react-select/creatable";

export default class ReactSelect extends Component {
  constructor(props) {
    super(props);

    this.scalar = (!props.keyProp && !props.valueProp && !props.textProp);
    this.state = {
      currentValue: [],
      clear: true === props.clear
    }
  }

  componentDidUpdate(prevProps) {
    if(prevProps.clear !== this.props.clear)
      this.setState({ clear: true === this.props.clear });
  }

  regenCurrentValue() {
    // Creatable.onChange() might have added a newly-created option, so check state.currentValue
    let selectedOption = [];
    if(this.state && !this.state.clear && this.state.currentValue != null)
      selectedOption = this.state.currentValue;
    // selectedOption is either the single option element or an array of options (in the case of multi-select)

    // selectedVals is an array containing just the option values
    let selectedVals = [selectedOption.value];
    if(Array.isArray(selectedOption))
      selectedVals = selectedOption.map(function(op) {return op.value});

    // currentValue is an array containing the one or the multiple current values
    let currentValue = [];
    if(this.props.value != null)
      currentValue = Array.isArray(this.props.value) ? this.props.value : [this.props.value];

    const comp = this;
    if(comp.scalar === true) {
      currentValue.forEach(val => {
        comp.setOrAddUniqueOption(selectedOption, selectedVals, val, val);
      });
    } else {
      if(false === this.props.objectsOrdering) {
        currentValue.forEach(curVal => {
          let index = comp.props.objects.findIndex(x => x[comp.props.valueProp] === curVal);
          if(index > -1) {
            let object = comp.props.objects[index];
            comp.setOrAddUniqueOption(selectedOption, selectedVals, object[comp.props.valueProp], object[comp.props.textProp]);
          }
        });
      } else {
        comp.props.objects.forEach(object => {
          if(currentValue.includes(object[comp.props.valueProp]))
            comp.setOrAddUniqueOption(selectedOption, selectedVals, object[comp.props.valueProp], object[comp.props.textProp]);
        });
      }
    }
    return selectedOption;
  }

  setOrAddUniqueOption(selectedOption, selectedVals, optionValue, optionText) {
    if(!selectedVals.includes(optionValue))
      if(Array.isArray(selectedOption))
        selectedOption.push({value: optionValue, label: optionText});
      else
        selectedOption = {value: optionValue, label: optionText};
  }

  render() {
    let currentValue = this.regenCurrentValue();
    let comp = this;
    const options = [];
    let maxLength = 1;

    if(true === this.props.includeEmptyOption)
      options.push({value: "", label: ""});
    this.props.objects.forEach(item => {
      if(comp.scalar) {
        options.push({value: item, label: item});
        maxLength = Math.max(maxLength, item.length);
      } else {
        if(item.active === true || item.active === undefined) {
          options.push({value: item[comp.props.valueProp], label: item[comp.props.textProp]});
          maxLength = Math.max(maxLength, item[comp.props.textProp].length);
        }
      }
    });

    let customStyles = {
      input: (provided, state) => ({
        minWidth: maxLength*3/5 + "em"
      })
    }
    let rSelect = null;
    let disabled = typeof this.props.disabled === "function" ? this.props.disabled(this.props) : this.props.disabled;
    if(true === this.props.isCreatable) {
      rSelect = (
        <CreatableSelect className={this.props.className} styles={customStyles} name={this.props.selectId} value={currentValue}
          options={options} autoFocus={true === this.props.autoFocus} disabled={true === disabled}
          isMulti={true === this.props.isMulti} isSearchable={!(false === this.props.isSearchable)}
          onChange={(selectedOption) => {this.rsChangeHandler(this.props.selectId, selectedOption, this.props.onChange)}} />
      );
    } else {
       rSelect = (
        <Select className={this.props.className} styles={customStyles} name={this.props.selectId} value={currentValue}
          options={options} autoFocus={true === this.props.autoFocus} disabled={true === disabled}
          isMulti={true === this.props.isMulti} isSearchable={!(false === this.props.isSearchable)}
          onChange={(selectedOption) => {this.rsChangeHandler(this.props.selectId, selectedOption, this.props.onChange)}} />
      );
    }

    if(false === this.props.vertical)
      return (
        <label className={this.props.className}>
            {this.props.label && (
              <span>{this.props.label}:&nbsp;</span>
            )}
            {rSelect}
          {this.props.errors && this.props.errors[this.props.selectId] && (
            <label htmlFor={this.props.selectId} className="errorLabel">{this.props.errors[this.props.selectId]}</label>
          )}
        </label>
      );

    return (
      <li className={this.props.className}>
        {this.props.label &&
          <label>{this.props.label}:&nbsp;</label>
        }
        {rSelect}
        {this.props.errors && this.props.errors[this.props.selectId] && (
          <div>
            <label htmlFor={this.props.selectId} className="errorLabel">{this.props.errors[this.props.selectId]}</label>
          </div>
        )}
      </li>
    );
  }

  rsChangeHandler(elemId, selectedOption, changeHandler) {
    this.setState({ currentValue: selectedOption });
    if(!changeHandler)
      return;
    if(selectedOption == null)
      changeHandler({
        target: {
          id: elemId,
          value: []
        }
      });
    else
      if(Array.isArray(selectedOption))
        changeHandler({
          target: {
            id: elemId,
            value: selectedOption.map(function(op) {return op.value}).join()
          }
        });
      else
        changeHandler({
          target: {
            id: elemId,
            value: selectedOption.value
          }
        });
  }

}

ReactSelect.propTypes = {
  label: PropTypes.string,
  selectId: PropTypes.string.isRequired,
  value: PropTypes.any,                   // the default value
  objects: PropTypes.array.isRequired,
  onChange: PropTypes.func,
  includeEmptyOption: PropTypes.bool,     // default false - whether to insert an empty option as the first element
  keyProp: PropTypes.string,              // keyProp, valueProp, and textProp properties refer to object properties
  valueProp: PropTypes.string,            // if keyProp, valueProp, or textProp is null, the object is treated as a scalar
  textProp: PropTypes.string,
  errors: PropTypes.object,               // property (map) against which the input will be checked
  autoFocus: PropTypes.bool,              // default false - whether the generated input should be focused
  isSearchable: PropTypes.bool,           // default true - whether option filter is enabled
  isMulti: PropTypes.bool,                // default false - whether this is a multi-select
  isCreatable: PropTypes.bool,            // default false - whether to allow creating a new option
  clear: PropTypes.bool,                  // default false - whether the current selection should be cleared
  className: PropTypes.string,            // optional class to apply
  vertical: PropTypes.bool,               // default true - whether to render in an li
  disabled: PropTypes.any,                // a boolean or a function that returns a boolean
  objectsOrdering: PropTypes.bool,        // default true - whether selected values should be ordered the same as the objects (rather than retain their order)
}

