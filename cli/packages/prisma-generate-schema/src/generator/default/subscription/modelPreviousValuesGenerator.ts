import { ModelObjectTypeGenerator } from '../../generator'
import { IGQLType, IGQLField } from 'prisma-datamodel'

export default class ModelPreviousValuesGenerator extends ModelObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}PreviousValues`
  }

  protected generateScalarFieldType(
    model: IGQLType,
    args: {},
    field: IGQLField,
  ) {
    return this.generators.scalarTypeGenerator.mapToScalarFieldType(field)
  }

  protected generateRelationFieldType(
    model: IGQLType,
    args: {},
    field: IGQLField,
  ) {
    return null
  }
}
