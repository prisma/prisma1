import { GraphQLEnumType, GraphQLEnumValueConfigMap } from 'graphql/type'
import { IGQLType } from 'prisma-datamodel'
import { ModelEnumTypeGeneratorBase } from '../../generator'
import GQLAssert from '../../../util/gqlAssert'

export default class ModelEnumTypeGenerator extends ModelEnumTypeGeneratorBase {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}`
  }

  protected generateInternal(input: IGQLType, args: {}) {
    const enumValues = {} as GraphQLEnumValueConfigMap

    for (const field of input.fields) {
      enumValues[`${field.name}`] = field.defaultValue !== null ? { value: field.defaultValue } : {}
    }

    return new GraphQLEnumType({
      name: this.getTypeName(input, args),
      values: enumValues,
    })
  }
}
