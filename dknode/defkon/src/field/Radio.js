import React, { Component } from "react";
import PropTypes from "prop-types";

// optional errors property (map) against which the input will be checked
export default class Radio extends Component {
  render() {
    let disabled = typeof this.props.disabled === "function" ? this.props.disabled(this.props) : this.props.disabled;
    return (
      <li className={this.props.className}>
        <input id={this.props.selectId} type="radio" name={this.props.name} checked={this.props.value}
               disabled={true === disabled} onChange={this.props.onChange}/>
        <label htmlFor={this.props.selectId}>{this.props.label}</label>
        {this.props.errors && this.props.errors[this.props.selectId] && (
          <label htmlFor={this.props.selectId} className="errorLabel">{this.props.errors[this.props.selectId]}</label>
        )}
      </li>
    );
  }
}

Radio.propTypes = {
  label: PropTypes.string.isRequired,
  selectId: PropTypes.string.isRequired,
  name: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  errors: PropTypes.object,
  value: PropTypes.any,
  className: PropTypes.string,            // optional class to apply
  disabled: PropTypes.any,                // a boolean or a function that returns a boolean
}

