import EntitySelectCellEditor from "./EntitySelectCellEditor";

export default class EntitySelectCellEditorCreatableEmpty extends EntitySelectCellEditor {
  constructor(props) {
    super(props);
    this.init({isCreatable: true, includeEmptyOption: true});
  }
}
