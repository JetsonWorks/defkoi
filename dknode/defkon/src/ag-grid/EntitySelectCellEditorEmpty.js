// for custom edit, maybe extend PopupSelectCellEditor and override init()
// ref: https://www.ag-grid.com/react-grid/component-cell-editor/

import EntitySelectCellEditor from "./EntitySelectCellEditor";

// includes empty option
export default class EntitySelectCellEditorEmpty extends EntitySelectCellEditor {
  constructor(props) {
    super(props);
    this.init({includeEmptyOption: true});
  }
}
