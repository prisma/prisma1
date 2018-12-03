import { IGQLField } from 'prisma-datamodel'
import { ScalarTypeGeneratorBase } from '../generator/generator'
import { GQLAssert as BaseGQLAssert } from 'prisma-datamodel'

export default abstract class GQLAssert extends BaseGQLAssert {
  public static isScalar(field: IGQLField, generator: ScalarTypeGeneratorBase) {
    GQLAssert.raiseIf(
      !generator.isScalarField(field),
      `Expected scalar type but got ${field.type}`,
    )
  }

  public static isRelation(
    field: IGQLField,
    generator: ScalarTypeGeneratorBase,
  ) {
    GQLAssert.raiseIf(
      generator.isScalarField(field),
      `Expected relation type but got ${field.type}`,
    )
  }
}
