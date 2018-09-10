import { GraphQLEnumType, GraphQLEnumValueConfigMap, GraphQLID, GraphQLDate } from 'graphql/type'
import { IGQLType } from '../../datamodel/model'
import { ModelEnumTypeGeneratorBase } from '../generator'

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

    // These fields are always present. 
    values.id_ASC = { type: this.generators.scalarTypeGenerator.generate('ID', {}) }
    values.id_DESC = { type: this.generators.scalarTypeGenerator.generate('ID', {}) }
    values.createdAt_ASC = { type: this.generators.scalarTypeGenerator.generate('DateTime', {}) }
    values.createdAt_DESC = { type: this.generators.scalarTypeGenerator.generate('DateTime', {}) }
    values.updatedAt_ASC = { type: this.generators.scalarTypeGenerator.generate('DateTime', {}) }
    values.updatedAt_DESC = { type: this.generators.scalarTypeGenerator.generate('DateTime', {}) }


    return new GraphQLEnumType({
      name: this.getTypeName(input, args),
      values,
    })
  }
}
