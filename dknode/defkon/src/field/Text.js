import React, { Component } from "react";
import PropTypes from "prop-types";

// optional errors property (map) against which the input will be checked
export default class Text extends Component {
  render() {
    let defaultValue = "";
    if(this.props.value)
      defaultValue = this.props.value;
    else if("function" === typeof this.props.defaultValueFunc)
      defaultValue = this.props.defaultValueFunc();
    let disabled = typeof this.props.disabled === "function" ? this.props.disabled(this.props) : this.props.disabled;
    let input = <input id={this.props.selectId} type={this.props.inputType ? this.props.inputType : "text"} defaultValue={defaultValue}
                       disabled={true === disabled} onChange={this.props.onChange} style={this.props.style}/>
    if(this.props.rows && Number(this.props.rows) > 1)
      input = <textarea id={this.props.selectId} rows={this.props.rows} defaultValue={defaultValue} onChange={this.props.onChange} style={this.props.style}/>

    if(false === this.props.vertical)
      return (
        <label className={this.props.className} style={this.props.style}>
          {this.props.label && (
            <label htmlFor={this.props.selectId}>{this.props.label}:&nbsp;</label>
          )}
          {input}
          {this.props.errors && this.props.errors[this.props.selectId] && (
            <label htmlFor={this.props.selectId} className="errorLabel">{this.props.errors[this.props.selectId]}</label>
          )}
        </label>
      );

    return (
      <li className={this.props.className} style={this.props.style}>
        <label htmlFor={this.props.selectId}>{this.props.label}:&nbsp;</label>
        {input}
        {this.props.errors && this.props.errors[this.props.selectId] && (
          <label htmlFor={this.props.selectId} className="errorLabel">{this.props.errors[this.props.selectId]}</label>
        )}
      </li>
    );
  }
}

Text.propTypes = {
  label: PropTypes.string,
  selectId: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  errors: PropTypes.object,
  value: PropTypes.any,
  defaultValueFunc: PropTypes.func,       // function to calculate the default value
  className: PropTypes.string,            // optional class to apply
  rows: PropTypes.number,                 // rows > 1 will result in textarea being used
  vertical: PropTypes.bool,               // default true - whether to render in an li
  inputType: PropTypes.string,
  disabled: PropTypes.any,                // a boolean or a function that returns a boolean
}

