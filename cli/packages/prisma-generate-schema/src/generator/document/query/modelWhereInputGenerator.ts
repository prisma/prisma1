import { IGQLType, IGQLField } from '../../../datamodel/model'
import RelationalModelWhereInputGenerator from '../../default/query/modelWhereInputGenerator'
import { GraphQLInputFieldConfigMap } from 'graphql'

export default class ModelWhereInputGenerator extends RelationalModelWhereInputGenerator {
  public generateRelationFilterFields(model: IGQLType, field: IGQLField): GraphQLInputFieldConfigMap | null {
    // Can only filter for embedded types, or on embedded types. 
    if((field.type as IGQLType).isEmbedded || model.isEmbedded) {
      return super.generateRelationFilterFields(model, field)
    } else {
      return null
    }
  }
}