import { IGQLType, IGQLField } from 'prisma-datamodel'
import RelationalModelWhereInputGenerator from '../../default/query/modelWhereInputGenerator'
import { GraphQLInputFieldConfigMap } from 'graphql'

export default class ModelRestrictedWhereInputGenerator extends RelationalModelWhereInputGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}RestrictedWhereInput`
  }

  public generateRelationFilterFields(model: IGQLType, field: IGQLField): GraphQLInputFieldConfigMap | null {
    // Can only filter for embedded types, or on embedded types when in restricted where type.
    if((field.type as IGQLType).isEmbedded) {
      return super.generateRelationFilterFields(model, field)
    } else {
      return null
    }
  }

  protected getLogicalOperators() : string[] {
    return ['AND']
  }
}