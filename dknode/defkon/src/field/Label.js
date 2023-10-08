import React, { Component } from "react";
import PropTypes from "prop-types";

export default class Label extends Component {
  render() {
    if(false === this.props.vertical)
      return (
        <label>
          {this.props.label && (
            <span htmlFor={this.props.selectId}>{this.props.label}:&nbsp;</span>
          )}
          <span id={this.props.selectId}>{this.props.value ? this.props.value : ""}</span>
        </label>
      );

    return (
      <li>
        <label htmlFor={this.props.selectId}>{this.props.label}:&nbsp;</label>
        <label id={this.props.selectId}>{this.props.value ? this.props.value : ""}</label>
      </li>
    );
  }
}

Label.propTypes = {
  label: PropTypes.string.isRequired,
  selectId: PropTypes.string.isRequired,
  value: PropTypes.any,
  vertical: PropTypes.bool,               // default true - whether to render in an li
}

