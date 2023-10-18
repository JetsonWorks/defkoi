import PropTypes from "prop-types";

// helper of entity values (e.g. entity is a pipeConf and nested entity is a device)
// keys entities by ID (for caching) and takes other parameters from colDef.colId and colDef.field
// dataKey (the name of the entity's property being edited)
//   defaults to parent dot-property of colDef.colId (e.g. "device" from device.displayName)
// labelKey (the nested property's key for the display value)
//   defaults to nested dot-property of colDef.colId (e.g. "displayName" from device.displayName)
// formKey (the name of the request parameter)
//   defaults to parent dot-property of colDef.field (e.g. "deviceId" from deviceId.id)
// valueKey (the nested property's key for the request value(s))
//   defaults to nested dot-property of colDef.field (e.g. "id" from deviceId.id)
export default class EntityCellEditorHelper {
  constructor(entityType, objects, entityPatcher, cacheUpdater, cacheRefresher, options) {
    this.entityType = entityType;
    this.objects = objects;
    this.patcher = entityPatcher;
    this.cacheUpdater = cacheUpdater;
    this.cacheRefresher = cacheRefresher;
    this.labelFormatter = entity => entity ? String(entity[this.labelKey]) : null;
    this.isCreatable = false;
    this.postValueChangesToParams = false;
    this.sort = true;

    if(options) {
      ["dataKey", "labelKey", "formKey", "valueKey", "labelFormatter", "isCreatable", "postValueChangesToParams", "sort"].forEach(key => {
        if(Object.keys(options).includes(key)) {
          this[key] = options[key];
        }
      });
    }

    this.valueGetter = this.valueGetter.bind(this);
    this.getValue = this.getValue.bind(this);
    this.valueFormatter = this.valueFormatter.bind(this);
    this.formatEntityValue = this.formatEntityValue.bind(this);
    this.formatValue = this.formatValue.bind(this);
    this.prepareUpdate = this.prepareUpdate.bind(this);
    this.valueSetter = this.valueSetter.bind(this);
    this.getCellEditorParams = this.getCellEditorParams.bind(this);
    this.valuePoster = this.valuePoster.bind(this);
    this.comparator = this.comparator.bind(this);

    // needed for values (from valueGetter()) that are not scalar
    this.filterParams = {
      textFormatter: this.formatValue,
      textCustomComparator: this.textComparator,
    };
  }

  // for passing the current values into cell editor
  valueGetter(params) {
    if(!this.dataKey || !this.labelKey) {
      if(!params.colDef.colId)
        throw new Error("Error: must set colId" + (params.colDef.field ? " for field " + params.colDef.field : ""));
      let pcs = params.colDef.colId.split(".");
      if(!this.dataKey)
        this.dataKey = pcs[0];
      if(!this.labelKey)
        this.labelKey = pcs[1];
    }
    if(!this.formKey || !this.valueKey) {
      if(!params.colDef.field)
        throw new Error("Error: must set field" + (params.colDef.colId ? " for colId " + params.colDef.colId : ""));
      let pcs = params.colDef.field.split(".");
      if(!this.formKey)
        this.formKey = pcs[0];
      if(!this.valueKey)
        this.valueKey = pcs[1];
    }
    return this.getValue(params.data);
  }

  getValue(entity) {
    return entity[this.dataKey];
  }

  comparator(valueA, valueB, nodeA, nodeB, isInverted) {
    if(!valueA || valueA === null)
      return !valueB || valueB === null ? 0 : -1;
    if(!valueB || valueB === null)
      return 1;
    return this.formatValue(valueA).localeCompare(this.formatValue(valueB));
  }

  // for displaying the current values
  valueFormatter(params) {
    return this.formatEntityValue(params.data);
  }

  formatEntityValue(entity) {
    return this.formatValue(this.getValue(entity));
  }

  // this is also called by filtering functionality, not only on the values being filtered, but the filterText itself
  formatValue(value) {
    if(typeof value === "string")
      return value;
    if(!value || value === null)
      return null;
    if(Array.isArray(value)) {
      let formatted = value.filter(x => x.active === true || x.active === undefined)
        .map(x => this.labelFormatter(x));
      return this.sort === true ? formatted.sort().join(", ") : formatted.join(", ");
    } else {
      return this.labelFormatter(value);
    }
  }

  // https://www.ag-grid.com/react-data-grid/filter-text/
  textComparator(filter, value, filterText) {
    const filterTextLowerCase = filterText.toLowerCase();
    const valueLowerCase = value.toString().toLowerCase();
    switch (filter) {
      case 'contains':
        return valueLowerCase.indexOf(filterTextLowerCase) >= 0;
      case 'notContains':
        return valueLowerCase.indexOf(filterTextLowerCase) === -1;
      case 'equals':
        return valueLowerCase === filterTextLowerCase;
      case 'notEqual':
        return valueLowerCase !== filterTextLowerCase;
      case 'startsWith':
        return valueLowerCase.indexOf(filterTextLowerCase) === 0;
      case 'endsWith':
        var index = valueLowerCase.lastIndexOf(filterTextLowerCase);
        return index >= 0 && index === (valueLowerCase.length - filterTextLowerCase.length);
      default:
        // should never happen
        console.warn('invalid filter type ' + filter);
        return false;
    }
  }

  // note: valueSetter() takes output from editor (array of select options) and updates data in the current row, but
  // since we're using Redux and the data is immutable, we need to dispatch an action, so
  // this prepares the action payload
  prepareUpdate(params) {
    let map = new Map();
    this.objects.forEach(ent => {
      map.set(ent[this.valueKey], ent);
    });

    let value;
    if(Array.isArray(this.valueGetter(params))) {
      if(params.newValue) {
        value = [];
        if(!Array.isArray(params.newValue)) {
          throw new Error("Error: single value returned from cell editor when an array was expected. Did you specify an EntityMultiCellEditor in the column?");
        }
        params.newValue.forEach(val => {
          let obj = map.get(val.value);
          if(!obj) {
            if(!this.isCreatable) {
              throw new Error("Warning: no entity mapped with value '" + params.newValue.value + "'");
            } else {
              obj = {};
              obj[this.valueKey] = val.value;
              map.set(val.value, obj);
            }
          }
          if(val.value)
            value.push(map.get(val.value))
        });
      } else {
        value = [];
      }
    } else {
      if(params.newValue.value === undefined || params.newValue.value === null) return null;
      if(params.oldValue && params.oldValue.id === params.newValue.value) return null;
      if(params.newValue.value !== "")
        value = map.get(params.newValue.value);
      if(!value) {
        if(params.newValue.value !== "" && !this.isCreatable) {
          throw new Error("Warning: no entity mapped with value '" + params.newValue.value + "'");
        } else {
          value = {};
          value[this.labelKey] = params.newValue.value;
        }
      }
    }
    return {
      type: this.entityType,
      id: params.data.id,
      data: params.data,
      dataKey: this.dataKey,
      formKey: this.formKey,
      value,
    };
  }

  // takes ValueSetterParams
  valueSetter(params) {
    // update the Redux state
    let update = this.prepareUpdate(params);
    if(!update) return;
    this.cacheUpdater(update);

    // in the meantime, replace this row data element with a clone with the updated value (for valuePoster)
    let temp = {...params.data};
    temp[update.dataKey] = update.value;
    params.node.updateData(temp);
  }

  // list of objects for select options
  getCellEditorParams() {
    return {values: this.objects.filter(x => x.active === true || x.active === undefined)};
  }

  // the onCellValueChanged callback, takes CellValueChangedParams, delegates to entityPatcher
  valuePoster(params) {
    if(!params.data[this.dataKey]) return;
    if(this.postValueChangesToParams && this.postValueChangesToParams === true)
      params.node.data[this.dataKey] = params.data[this.dataKey];
    let dataElem = params.data[this.dataKey];
    if(Array.isArray(dataElem) && dataElem.some(x => x === undefined))
      throw new Error("Error: not all values were mapped to objects. Are dataKey and valueKey correct? Did you choose the right cell editor? Were the objects loaded?");
    let promise;
    if(Array.isArray(dataElem)) {
      // abort if nothing changed
      if(params.oldValue.length === 0 && params.newValue.length === 0) return;
      if(params.oldValue.length === params.newValue.length) {
        let same = true;
        params.oldValue.forEach((elem, index) => {
          if(same && elem.id !== params.newValue[index].id)
            same = false;
        });
        if(same) return;
      }
      if(dataElem.length === 0) {
        promise = this.patcher(params.data, this.formKey + "Clear", true);
      } else {
        promise = this.patcher(params.data, this.formKey, dataElem.map(x => x[this.valueKey]).join(","));
      }
    } else {
      if(params.oldValue?.id && params.newValue?.id && params.oldValue.id === params.newValue.id) return;
      if(this.isCreatableValueEmpty(params.oldValue) && this.isCreatableValueEmpty(params.newValue)) return;
      promise = this.patcher(params.data, this.formKey, dataElem && dataElem[this.valueKey] ? dataElem[this.valueKey] : "");
    }
    if(promise && promise.done)
      promise.done(() => this.cacheRefresher({type: this.entityType}));
  }

  isCreatableValueEmpty(value) {
    if(!value) return true;
    return (!value[this.valueKey] || value[this.valueKey] === "") && (!value[this.labelKey] || value[this.labelKey] === "");
  }

}
EntityCellEditorHelper.propTypes = {
  entityType: PropTypes.string.isRequired,       // the type of entity being edited
  objects: PropTypes.array.isRequired,           // the array of objects to populate the select options
  entityPatcher: PropTypes.func.isRequired,      // the BaseGrid patch function
  cacheUpdater: PropTypes.func.isRequired,       // the dispatch function to update the Redux cache
  cacheRefresher: PropTypes.func.isRequired,     // the dispatch function to query the cache collection
  options: PropTypes.object,                     // modifiers, such as isCreatable
}

