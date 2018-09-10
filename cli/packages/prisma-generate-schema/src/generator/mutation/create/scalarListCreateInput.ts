import { ScalarInputGenerator } from "../../generator"
import { IGQLType, IGQLField } from "../../../datamodel/model"
import { GraphQLInputObjectType } from 'graphql/type'

export default class ScalarListCreateInput extends ScalarInputGenerator {
  public getTypeName(input: IGQLType, args: IGQLField) {
    return `${input.name}Create${args.name}Input`
  }
  protected generateInternal(input: IGQLType, args: IGQLField) {
    return new GraphQLInputObjectType({
      name: this.getTypeName(input, args),
      fields: {
        set: {
          type: this.generators.scalarTypeGenerator.mapToScalarFieldTypeForceOptional(args)
        }
      }
    })
  }
}