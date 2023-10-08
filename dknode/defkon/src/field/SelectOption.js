import React, { Component } from "react";
import PropTypes from "prop-types";

// set the optional includeEmptyOption property to insert an empty option as the first element
// optional value is displayed as the default value
// optional keyProp, valueProp, and textProp properties refer to object properties
// if keyProp, valueProp, or textProp is null, the object is treated as a scalar
// optional errors property (map) against which the input will be checked
export default class SelectOption extends Component {
  constructor(props) {
    super(props);
    this.scalar = (!props.keyProp && !props.valueProp && !props.textProp);
  }

  render() {
    let currentValue = this.state && this.state.currentValue ? this.state.currentValue : this.props.value;
    let comp = this;
    let disabled = typeof this.props.disabled === "function" ? this.props.disabled(this.props) : this.props.disabled;
    return (
      <li>
        <label htmlFor={this.props.selectId}>{this.props.label}:&nbsp;</label>
        <select id={this.props.selectId} value={currentValue} disabled={true === disabled}
                onChange={(selectedOption) => {
                  this.changeHandler(this.props.selectId, selectedOption, this.props.onChange)
                }}>
          {this.props.includeEmptyOption && (
            <option value=""></option>
          )}
          {this.props.objects.map(function (item, index) {
            if(comp.scalar)
              return (
                <option key={index} value={item}>{item}</option>
              );
            else
              return (
                <option key={(comp.props.keyPrefix ? comp.props.keyPrefix : "") + item[comp.props.keyProp]} value={"" + item[comp.props.valueProp]}>
                  {"" + item[comp.props.textProp]}</option>
              );
          })}
        </select>
        {this.props.errors && this.props.errors[this.props.selectId] && (
          <div>
            <label htmlFor={this.props.selectId} className="errorLabel">{this.props.errors[this.props.selectId]}</label>
          </div>
        )}
      </li>
    );
  }

  changeHandler(elemId, event, changeHandler) {
    let selectedOption = event.target.value;
    this.setState({currentValue: selectedOption});
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
      changeHandler({
        target: {
          id: elemId,
          value: selectedOption
        }
      });
  }

}

SelectOption.propTypes = {
  label: PropTypes.string.isRequired,
  selectId: PropTypes.string.isRequired,
  value: PropTypes.string,
  objects: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
  includeEmptyOption: PropTypes.bool,
  keyProp: PropTypes.string,
  valueProp: PropTypes.string,
  textProp: PropTypes.string,
  errors: PropTypes.object,
  disabled: PropTypes.any,                // a boolean or a function that returns a boolean
}

