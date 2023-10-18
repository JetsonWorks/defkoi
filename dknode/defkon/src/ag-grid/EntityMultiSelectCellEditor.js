import EntitySelectCellEditor from "./EntitySelectCellEditor";

export default class EntityMultiSelectCellEditor extends EntitySelectCellEditor {
  constructor(props) {
    super(props);
    this.init({isMulti: true});
  }
}
