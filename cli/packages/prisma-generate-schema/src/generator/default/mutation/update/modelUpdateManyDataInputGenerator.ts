import { IGQLType, IGQLField } from 'prisma-datamodel'
import ModelUpdateInputGenerator from './modelUpdateInputGenerator'
import { TypeFromModelGenerator } from '../../../generator'

export default class ModelUpdateManyDataInputGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateManyDataInput`
  }

  public wouldBeEmpty(model: IGQLType, args: {}) {
    return !this.hasWriteableFields(this.getScalarFields(model.fields))
  }

  protected generateRelationField(model: IGQLType, args: {}, field: IGQLField) {
    return null
  }
}
