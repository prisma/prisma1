import { IGQLField, IGQLType } from '../../datamodel/model'
import { GraphQLInputFieldConfigMap } from "graphql/type"
import ModelWhereInputGenerator from './modelWhereInputGenerator'

export default class ModelScalarWhereInputGenerator extends ModelWhereInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}ScalarWhereInput`
  }

  public generateRelationFilterFields(field: IGQLField): GraphQLInputFieldConfigMap | null {
    return null;
  }
}