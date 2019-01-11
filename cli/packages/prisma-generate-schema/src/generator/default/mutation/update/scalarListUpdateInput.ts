import { ModelInputObjectTypeGenerator } from "../../../generator"
import { IGQLType, IGQLField } from 'prisma-datamodel'
import { GraphQLInputObjectType } from 'graphql/type'

export default class ScalarListUpdateInput extends ModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: IGQLField) {
    return `${input.name}Update${args.name}Input`
  }
  protected generateInternal(input: IGQLType, args: IGQLField) {
    return new GraphQLInputObjectType({
      name: this.getTypeName(input, args),
      fields: {
        set: {
          type: this.generators.scalarTypeGenerator.wrapList(
            this.generators.scalarTypeGenerator.generate(args.type, {})
          )
        }
      }
    })
  }
}