import { IGQLField } from "../datamodel/model";
import { ScalarTypeGeneratorBase } from "../generator/generator";


export default abstract class GQLAssert {
  public static raise(message: string) {
    throw new Error(message)
  }
  public static raiseIf(condition: boolean, message: string) {
    if(condition) {
      GQLAssert.raise(message)
    }
  }

  public static isScalar(field: IGQLField, generator: ScalarTypeGeneratorBase) {
    GQLAssert.raiseIf(!generator.isScalarField(field), `Expected scalar type but got ${field.type}`)
  }

  public static isRelation(field: IGQLField, generator: ScalarTypeGeneratorBase) {
    GQLAssert.raiseIf(generator.isScalarField(field), `Expected relation type but got ${field.type}`)
  }
}