import React, { Component } from "react";
import PropTypes from "prop-types";
import classNames from "classnames";

// left-right, red-yellow buttons, acting as a toggle
// optional errors property (map) against which the input will be checked
export default class ToggleDualButton extends Component {
  render() {
    let on = this.props.value === undefined ? this.props.defaultValue : this.props.value;
    let disabled = typeof this.props.disabled === "function" ? this.props.disabled(this.props) : this.props.disabled;
    return (
      <li className={this.props.className}>
        {this.props.label && (
          <label htmlFor={this.props.selectId}>{this.props.label}:&nbsp;</label>
        )}
        <button className={classNames({"side-button": true, "left": true, "active": true !== on})}
                disabled={true === disabled} onClick={() => {
          if(true === disabled) return;
          this.props.onChange(this.props.selectId, false)
        }}>
          {this.props.off ? this.props.off : "Off"}</button>
        <button className={classNames({"side-button": true, "right": true, "active": true === on})}
                disabled={true === disabled} onClick={() => {
          if(true === disabled) return;
          this.props.onChange(this.props.selectId, false)
        }}>
          {this.props.on ? this.props.on : "On"}</button>
      </li>
    );
  }
}

ToggleDualButton.propTypes = {
  label: PropTypes.string,
  selectId: PropTypes.string.isRequired,
  name: PropTypes.string,                 // for multi-select
  onChange: PropTypes.func,
  errors: PropTypes.object,
  defaultValue: PropTypes.bool,           // the initial value
  value: PropTypes.bool,                  // the current (unsaved) value
  className: PropTypes.string,            // optional class to apply
  on: PropTypes.string,                   // text for on option
  off: PropTypes.string,                  // text for off option
  disabled: PropTypes.any,                // a boolean or a function that returns a boolean
}

