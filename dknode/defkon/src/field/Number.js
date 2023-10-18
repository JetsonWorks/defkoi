import React, { Component } from "react";
import PropTypes from "prop-types";

// optional errors property (map) against which the input will be checked
export default class Number extends Component {
  constructor(props) {
    super(props);
    this.state = {}
  }

  componentDidUpdate(prevProps) {
    if(prevProps.value !== this.props.value)
      this.setState({value: this.props.value});
  }

  render() {
    let val = this.props.value ? this.props.value : "";
    let disabled = typeof this.props.disabled === "function" ? this.props.disabled(this.props) : this.props.disabled;
    let input = true === this.props.controlled ?
      <input id={this.props.selectId} type="number" value={val} size={4}
             disabled={true === disabled} onChange={this.props.onChange}/> :
      <input id={this.props.selectId} type="number" defaultValue={val} size={4}
             disabled={true === disabled} onChange={this.props.onChange}/>
    return (
      <li>
        <label htmlFor={this.props.selectId}>{this.props.label}:&nbsp;</label>
        {input}
        {this.props.errors && this.props.errors[this.props.selectId] && (
          <label htmlFor={this.props.selectId} className="errorLabel">{this.props.errors[this.props.selectId]}</label>
        )}
      </li>
    );
  }
}

Number.propTypes = {
  label: PropTypes.string.isRequired,
  selectId: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  errors: PropTypes.object,
  value: PropTypes.any,
  disabled: PropTypes.any,                // a boolean or a function that returns a boolean
  controlled: PropTypes.bool,            // whether this input is controlled (uses value) or uncontrolled (default, uses defaultValue)
}

