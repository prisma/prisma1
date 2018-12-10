import { GraphQLEnumType, GraphQLEnumValueConfigMap, GraphQLID } from 'graphql/type'
import { IGQLType } from 'prisma-datamodel'
import { ModelEnumTypeGeneratorBase } from '../../generator'

export default class ModelOrderByInputGenerator extends ModelEnumTypeGeneratorBase {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}OrderByInput`
  }

  protected generateInternal(input: IGQLType, args: {}) {
    const values = {} as GraphQLEnumValueConfigMap

    for (const field of input.fields) {
      if (
        field.isList ||
        !this.generators.scalarTypeGenerator.isScalarField(field)
      ) {
        continue
      }

      values[`${field.name}_ASC`] = {}
      values[`${field.name}_DESC`] = {}
    }

    return new GraphQLEnumType({
      name: this.getTypeName(input, args),
      values,
    })
  }
}
