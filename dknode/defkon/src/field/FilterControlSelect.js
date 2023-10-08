import React, { Component } from "react";
import PropTypes from "prop-types";

// set the optional includeEmptyOption property to insert an empty option as the first element
// if keyProp, valueProp, or textProp is null, the object is treated as a scalar
// optional keyPrefix property
export default class FilterControlSelect extends Component {
  constructor(props) {
    super(props);
    this.state = {
      scalar: (!props.keyProp && !props.valueProp && !props.textProp)
    }
  }

  render() {
    let comp = this;
    return (
      <label>{this.props.label}:&nbsp;
        <select id={this.props.selectId} defaultValue={this.props.value} onChange={this.props.onChange}>
          {this.props.includeEmptyOption && (
          <option value=""></option>
          )}
          {this.props.objects.filter(x => x.active === true || x.active === undefined).map(function(item, index) {
            if(comp.state.scalar) {
              return (
                <option key={index} value={item}>{item}</option>
              );
            } else {
              if(!item[comp.props.keyProp]) {
                console.warn("Warning: item does not have property " + comp.props.keyProp);
                console.warn(item);
              }
              return (
                <option key={(comp.props.keyPrefix ? comp.props.keyPrefix : "") + item[comp.props.keyProp]} value={"" + item[comp.props.valueProp]}>
                  {"" + item[comp.props.textProp]}</option>
              );
            }
          })}
        </select>
      </label>
    );
  }
}

FilterControlSelect.propTypes = {
  label: PropTypes.string.isRequired,
  selectId: PropTypes.string.isRequired,
  objects: PropTypes.array.isRequired,
  includeEmptyOption: PropTypes.bool,
  keyProp: PropTypes.string,
  valueProp: PropTypes.string,
  textProp: PropTypes.string,
  keyPrefix: PropTypes.string,
  value: PropTypes.string,
  onChange: PropTypes.func,
}

