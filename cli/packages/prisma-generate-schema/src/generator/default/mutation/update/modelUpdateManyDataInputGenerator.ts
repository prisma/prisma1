import { IGQLType, IGQLField } from '../../../../datamodel/model'
import ModelUpdateInputGenerator from './modelUpdateInputGenerator';
import { TypeFromModelGenerator } from '../../../generator';


export default class ModelUpdateManyDataInputGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateManyDataInput`
  }

  public wouldBeEmpty(model: IGQLType, args: {}) {
    return !TypeFromModelGenerator.hasFieldsExcept(
      this.getScalarFields(model.fields), ...TypeFromModelGenerator.reservedFields
    )
  }

  protected generateRelationField(model: IGQLType, args: {}, field: IGQLField) {
    return null;
  }
}