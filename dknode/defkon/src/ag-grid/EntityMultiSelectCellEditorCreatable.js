import EntitySelectCellEditor from "./EntitySelectCellEditor";

export default class EntityMultiSelectCellEditorCreatable extends EntitySelectCellEditor {
  constructor(props) {
    super(props);
    this.init({isCreatable: true, isMulti: true});
  }
}
