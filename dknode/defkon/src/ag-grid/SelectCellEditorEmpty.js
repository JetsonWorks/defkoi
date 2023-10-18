// for custom edit, maybe extend PopupSelectCellEditor and override init()
// ref: https://www.ag-grid.com/react-grid/component-cell-editor/

import SelectCellEditor from "./SelectCellEditor";

// includes empty option
export default class SelectCellEditorEmpty extends SelectCellEditor {
  constructor(props) {
    super(props);
    this.includeEmptyOption = true;
  }
}
