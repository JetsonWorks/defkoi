import React, { Component } from "react";
import PropTypes from "prop-types";
import "../toggle-button.css";

export default class Toggle extends Component {
  render() {
    let on = this.props.value === undefined ? this.props.defaultValue : this.props.value;
    let disabled = typeof this.props.disabled === "function" ? this.props.disabled(this.props) : this.props.disabled;
    return (
      <li className={this.props.className} style={{textAlign: "left"}}>
        {this.props.label && (
          <label htmlFor={this.props.selectId}>{this.props.label}:&nbsp;</label>
        )}
        <input id={this.props.selectId} type="checkbox" className={"cm-toggle " + this.props.className}
               checked={true === on} disabled={true === disabled}
               onChange={() => this.props.onChange(this.props.selectId, !on)}/>
      </li>
    );
  }
}

Toggle.propTypes = {
  label: PropTypes.string,
  selectId: PropTypes.string.isRequired,
  onChange: PropTypes.func,
  errors: PropTypes.object,
  defaultValue: PropTypes.bool,           // the initial value
  value: PropTypes.bool,                  // the current (unsaved) value
  className: PropTypes.string,            // optional class to apply
  disabled: PropTypes.any,                // a boolean or a function that returns a boolean
}

