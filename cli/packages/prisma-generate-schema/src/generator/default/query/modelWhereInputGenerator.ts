import {
  ModelInputObjectTypeGenerator,
  RelatedGeneratorArgs,
  IGenerators,
  FieldConfigUtils,
} from '../../generator'
import { IGQLType, IGQLField, TypeIdentifiers } from 'prisma-datamodel'
import GQLAssert from '../../../util/gqlAssert'
import {
  GraphQLObjectType,
  GraphQLInputFieldConfigMap,
  GraphQLFieldConfig,
  GraphQLType,
  GraphQLList,
  GraphQLString,
} from 'graphql/type'
import { GraphQLInputType } from 'graphql'

export default class ModelWhereInputGenerator extends ModelInputObjectTypeGenerator {
  public static generateFiltersForSuffix(
    suffixes: string[],
    modelField: IGQLField | null,
    gqlType: GraphQLInputType,
  ) {
    const fields = {} as GraphQLInputFieldConfigMap

    for (const suffix of suffixes) {
      fields[`${modelField !== null ? modelField.name : ''}${suffix}`] = {
        type: gqlType,
      }
    }

    return fields
  }

  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}WhereInput`
  }

  //#region Scalar filter generator
  public generateScalarFilterFields(
    model: IGQLType,
    field: IGQLField,
  ): GraphQLInputFieldConfigMap {
    GQLAssert.isScalar(field, this.generators.scalarTypeGenerator)

    if (field.isList) {
      return {} as GraphQLInputFieldConfigMap
    }

    if (typeof field.type === 'string') {
      switch (field.type as string) {
        case TypeIdentifiers.string:
        case TypeIdentifiers.id:
        case TypeIdentifiers.uuid:
          return FieldConfigUtils.merge(
            this.generateBaseFilters(field),
            this.generateInclusionFilters(field),
            this.generateAlphanumericFilters(field),
            this.generateStringFilters(field),
          )
        case TypeIdentifiers.integer:
        case TypeIdentifiers.float:
        case TypeIdentifiers.dateTime:
          return FieldConfigUtils.merge(
            this.generateBaseFilters(field),
            this.generateInclusionFilters(field),
            this.generateAlphanumericFilters(field),
          )
        case TypeIdentifiers.boolean:
          return FieldConfigUtils.merge(this.generateBaseFilters(field))
        case TypeIdentifiers.json:
          return FieldConfigUtils.merge()
        default:
          GQLAssert.raise(
            `Type ${
              field.type
            } is not implemented by ModelWhereInputGenerator.generateScalarFilterFields.`,
          )
          return {} as GraphQLInputFieldConfigMap
      }
    } else if ((field.type as IGQLType).isEnum) {
      return FieldConfigUtils.merge(
        this.generateBaseFilters(field),
        this.generateInclusionFilters(field),
      )
    } else {
      GQLAssert.raise(
        `Type for filter generation was neither enum nor scalar type.`,
      )
      return {} as GraphQLInputFieldConfigMap
    }
  }

  public generateBaseFilters(field: IGQLField): GraphQLInputFieldConfigMap {
    const type = this.generators.scalarTypeGenerator.generate(field.type, {})
    return ModelWhereInputGenerator.generateFiltersForSuffix(
      ['', '_not'],
      field,
      type,
    )
  }

  public generateInclusionFilters(
    field: IGQLField,
  ): GraphQLInputFieldConfigMap {
    const type = this.generators.scalarTypeGenerator.wrapList(
      this.generators.scalarTypeGenerator.generate(field.type, {}),
    )
    return ModelWhereInputGenerator.generateFiltersForSuffix(
      ['_in', '_not_in'],
      field,
      type,
    )
  }

  public generateAlphanumericFilters(
    field: IGQLField,
  ): GraphQLInputFieldConfigMap {
    const type = this.generators.scalarTypeGenerator.generate(field.type, {})
    return ModelWhereInputGenerator.generateFiltersForSuffix(
      ['_lt', '_lte', '_gt', '_gte'],
      field,
      type,
    )
  }

  public generateStringFilters(field: IGQLField): GraphQLInputFieldConfigMap {
    const type = this.generators.scalarTypeGenerator.generate(field.type, {})
    const filters = [
      '_contains',
      '_not_contains',
      '_starts_with',
      '_not_starts_with',
      '_ends_with',
      '_not_ends_with',
    ]
    return ModelWhereInputGenerator.generateFiltersForSuffix(
      filters,
      field,
      type,
    )
  }
  //#endregion

  //#region Relation filter generator

  public generateRelationFilterFields(
    model: IGQLType,
    field: IGQLField,
  ): GraphQLInputFieldConfigMap | null {
    GQLAssert.isRelation(field, this.generators.scalarTypeGenerator)
    if (field.isList) {
      return this.generateManyRelationFilterFields(field)
    } else {
      return this.generateOneRelationFilterFields(field)
    }
  }

  public generateOneRelationFilterFields(
    field: IGQLField,
  ): GraphQLInputFieldConfigMap {
    const type = this.generate(field.type as IGQLType, {})
    return ModelWhereInputGenerator.generateFiltersForSuffix([''], field, type)
  }

  public generateManyRelationFilterFields(
    field: IGQLField,
  ): GraphQLInputFieldConfigMap {
    const type = this.generate(field.type as IGQLType, {})
    return ModelWhereInputGenerator.generateFiltersForSuffix(
      this.getRelationManyFilters(field.type as IGQLType),
      field,
      type,
    )
  }

  //#endregion

  protected getRelationManyFilters(type: IGQLType): string[] {
    return ['_every', '_some', '_none']
  }

  protected getLogicalOperators(): string[] {
    return ['AND', 'OR', 'NOT']
  }

  protected generateFields(
    model: IGQLType,
    args: {},
  ): GraphQLInputFieldConfigMap {
    let fields = {} as GraphQLInputFieldConfigMap

    for (const field of model.fields) {
      const fieldsToAdd = this.generators.scalarTypeGenerator.isScalarField(
        field,
      )
        ? this.generateScalarFilterFields(model, field)
        : this.generateRelationFilterFields(model, field)

      if (fieldsToAdd !== null) {
        fields = FieldConfigUtils.merge(fields, fieldsToAdd)
      }
    }

    const recursiveFilter = ModelWhereInputGenerator.generateFiltersForSuffix(
      this.getLogicalOperators(),
      null,
      this.generators.scalarTypeGenerator.wrapList(this.generate(model, {})),
    )

    fields = FieldConfigUtils.merge(fields, recursiveFilter)

    return fields
  }
}
