import { IGQLType, IGQLField } from '../../../../datamodel/model'
import ModelUpdateInputGenerator from './modelUpdateInputGenerator';


export default class ModelUpdateManyDataInputGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateManyDataInput`
  }

  protected generateRelationField(model: IGQLType, args: {}, field: IGQLField) {
    return null;
  }
}