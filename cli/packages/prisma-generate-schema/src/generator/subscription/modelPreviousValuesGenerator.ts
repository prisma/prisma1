import { ModelObjectTypeGenerator } from '../generator'
import { IGQLType, IGQLField } from '../../datamodel/model'
import { GraphQLObjectType, GraphQLNonNull, GraphQLFieldConfigMap, GraphQLFieldConfig, GraphQLList, GrqphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"

export default class ModelPreviousValuesGenerator extends ModelObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}PreviousValues`
  }

  protected generateScalarFieldType(model: IGQLType, args: {}, field: IGQLField) {
    return this.generators.scalarTypeGenerator.mapToScalarFieldType(field)
  }

  protected generateRelationFieldType(model: IGQLType, args: {}, field: IGQLField) {
    return null
  }
}