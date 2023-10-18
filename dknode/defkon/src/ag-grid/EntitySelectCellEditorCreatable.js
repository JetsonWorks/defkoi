import EntitySelectCellEditor from "./EntitySelectCellEditor";

export default class EntitySelectCellEditorCreatable extends EntitySelectCellEditor {
  constructor(props) {
    super(props);
    this.init({isCreatable: true});
  }
}
