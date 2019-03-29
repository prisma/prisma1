import {
  ModelObjectTypeGenerator,
  RelatedGeneratorArgs,
  IGenerators,
  ModelInputObjectTypeGenerator,
  TypeFromModelGenerator,
  RelatedModelInputObjectTypeGenerator,
} from '../../../generator'
import { IGQLType, IGQLField, IdStrategy } from 'prisma-datamodel'
import {
  GraphQLObjectType,
  GraphQLFieldConfigMap,
  GraphQLFieldConfig,
  GraphQLList,
  GraphQLInputObjectType,
  GraphQLString,
  GraphQLInputFieldConfigMap,
  GraphQLInputFieldConfig,
} from 'graphql/type'

export default class ModelCreateInputGenerator extends ModelInputObjectTypeGenerator {
  /**
   * Generates an create model input field for a relational type, handling the four cases many/one and with/without related type.
   * @param model
   * @param field
   * @param generators
   */
  public static generateRelationFieldForInputType(
    model: IGQLType,
    field: IGQLField,
    generators: IGenerators,
  ) {
    const relationInfo = {
      relatedField: field,
      relatedType: model,
      relationName: field.relationName,
    }
    const generator = ModelCreateInputGenerator.getGeneratorForRelation(
      model,
      field,
      generators,
    )
    if (generator.wouldBeEmpty(field.type as IGQLType, relationInfo)) {
      return null
    } else {
      const schemaField = generator.generate(
        field.type as IGQLType,
        relationInfo,
      )
      return generators.scalarTypeGenerator.requiredIf(
        field.isRequired,
        schemaField,
      )
    }
  }

  public static relationWouldBeEmpty(
    model: IGQLType,
    field: IGQLField,
    generators: IGenerators,
  ) {
    const relationInfo = {
      relatedField: field,
      relatedType: model,
      relationName: field.relationName,
    }
    const generator = ModelCreateInputGenerator.getGeneratorForRelation(
      model,
      field,
      generators,
    )
    return generator.wouldBeEmpty(field.type as IGQLType, relationInfo)
  }

  public static getGeneratorForRelation(
    model: IGQLType,
    field: IGQLField,
    generators: IGenerators,
  ): RelatedModelInputObjectTypeGenerator {
    if (field.relatedField !== null) {
      if (field.isList) {
        return generators.modelCreateManyWithoutRelatedInput
      } else {
        return generators.modelCreateOneWithoutRelatedInput
      }
    } else {
      if (field.isList) {
        return generators.modelCreateManyInput
      } else {
        return generators.modelCreateOneInput
      }
    }
  }

  // TODO: Make all those instance members.
  public static generateScalarFieldTypeForInputType(
    model: IGQLType,
    field: IGQLField,
    generators: IGenerators,
  ) {
    if (
      field.isReadOnly &&
      !(
        field.idStrategy === IdStrategy.Auto ||
        field.idStrategy === IdStrategy.None
      )
    ) {
      return null
    } else {
      if (field.isList) {
        return generators.scalarListCreateInput.generate(model, field)
      } else {
        return generators.scalarTypeGenerator.mapToScalarFieldTypeForInput(
          field,
        )
      }
    }
  }

  public wouldBeEmpty(model: IGQLType, args: {}) {
    return (
      !this.hasCreateInputFields(this.getScalarFields(model.fields)) &&
      this.getRelationFields(model.fields).every(field =>
        ModelCreateInputGenerator.relationWouldBeEmpty(
          model,
          field,
          this.generators,
        ),
      )
    )
  }

  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}CreateInput`
  }

  /**
   * Generates all fields of this type.
   * @param model
   * @param args
   */
  protected generateFields(model: IGQLType, args: {}) {
    const fields = {} as GraphQLInputFieldConfigMap

    const fieldList = this.getCreateInputFields(model.fields)
    for (const field of fieldList) {
      const isScalar = this.generators.scalarTypeGenerator.isScalarField(field)
      const fieldSchema: GraphQLInputFieldConfig | null = isScalar
        ? this.generateScalarField(model, args, field)
        : this.generateRelationField(model, args, field)

      if (fieldSchema !== null) {
        fields[field.name] = fieldSchema
      }
    }

    return fields
  }

  protected generateRelationFieldType(
    model: IGQLType,
    args: {},
    field: IGQLField,
  ) {
    return ModelCreateInputGenerator.generateRelationFieldForInputType(
      model,
      field,
      this.generators,
    )
  }

  protected generateScalarFieldType(
    model: IGQLType,
    args: {},
    field: IGQLField,
  ) {
    return ModelCreateInputGenerator.generateScalarFieldTypeForInputType(
      model,
      field,
      this.generators,
    )
  }
}
