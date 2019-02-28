import { IGQLType, IGQLField } from 'prisma-datamodel'
import RelationalModelWhereInputGenerator from '../../default/query/modelWhereInputGenerator'
import { GraphQLInputFieldConfigMap } from 'graphql'

export default class ModelWhereInputGenerator extends RelationalModelWhereInputGenerator {
  public generateManyRelationFilterFields(
    field: IGQLField,
  ): GraphQLInputFieldConfigMap {
    const fieldType = field.type as IGQLType

    const whereType = this.generate(fieldType, {})
    const restrictedWhereType = this.generators.modelRestrictedWhereInput.generate(
      fieldType,
      {},
    )

    if (fieldType.isEmbedded) {
      return {
        ...RelationalModelWhereInputGenerator.generateFiltersForSuffix(
          ['_some'],
          field,
          whereType,
        ),
        // In this special case, we switch to the restricted where type.
        ...RelationalModelWhereInputGenerator.generateFiltersForSuffix(
          ['_every', '_none'],
          field,
          restrictedWhereType,
        ),
      }
    } else {
      return RelationalModelWhereInputGenerator.generateFiltersForSuffix(
        ['_some'],
        field,
        whereType,
      )
    }
  }

  protected getLogicalOperators(): string[] {
    return ['AND']
  }

  protected getRelationaManyFilters(type: IGQLType): string[] {
    throw new Error('Not implemented, not needed.')
  }
}
