import {
  RelatedGeneratorArgs,
  TypeFromModelGenerator,
  ModelInputObjectTypeGenerator,
} from '../../../generator'
import { IGQLType, IGQLField } from '../../../../datamodel/model'
import ModelUpdateInputGenerator from './modelUpdateInputGenerator'

export default class ModelUpdateManyMutationInputGenerator extends ModelInputObjectTypeGenerator {
  public wouldBeEmpty(model: IGQLType, args: {}) {
    return !TypeFromModelGenerator.hasFieldsExcept(
      model.fields.filter(field => this.generators.scalarTypeGenerator.isScalarField(field)),
      ...TypeFromModelGenerator.reservedFields,
    )
  }

  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}UpdateManyMutationInput`
  }

  protected generateRelationFieldType(
    model: IGQLType,
    args: {},
    field: IGQLField,
  ) {
    return null
  }

  protected generateScalarFieldType(
    model: IGQLType,
    args: RelatedGeneratorArgs,
    field: IGQLField,
  ) {
    return ModelUpdateInputGenerator.generateScalarFieldTypeForInputType(
      model,
      field,
      this.generators,
    )
  }
}
