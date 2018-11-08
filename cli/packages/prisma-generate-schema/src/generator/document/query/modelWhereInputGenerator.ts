import { IGQLType, IGQLField } from '../../../datamodel/model'
import RelationalModelWhereInputGenerator from '../../default/query/modelWhereInputGenerator'
import { GraphQLInputFieldConfigMap } from 'graphql'

export default class ModelWhereInputGenerator extends RelationalModelWhereInputGenerator {
  public generateRelationFilterFields(field: IGQLField): GraphQLInputFieldConfigMap | null {
    // Can only filter for embedded types.
    if((field.type as IGQLType).isEmbedded) {
      return super.generateRelationFilterFields(field)
    } else {
      return null
    }
  }
}