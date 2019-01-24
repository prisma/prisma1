import { IGQLType, IGQLField } from 'prisma-datamodel'
import RelationalModelWhereInputGenerator from '../../default/query/modelWhereInputGenerator'
import { GraphQLInputFieldConfigMap } from 'graphql'

export default class ModelWhereInputGenerator extends RelationalModelWhereInputGenerator {
  public generateRelationFilterFields(model: IGQLType, field: IGQLField): GraphQLInputFieldConfigMap | null {
    // Can only filter for embedded types, or on embedded types. 
    if((field.type as IGQLType).isEmbedded) {
      return super.generateRelationFilterFields(model, field)
    } else {
      return null
    }
  }

  protected getSupportedLogicalOperators() {
    return ['AND']
  }
}