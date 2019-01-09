import { ModelObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, ModelInputObjectTypeGenerator, TypeFromModelGenerator, RelatedModelInputObjectTypeGenerator } from '../../../generator'
import { IGQLType, IGQLField } from 'prisma-datamodel'
import { GraphQLObjectType, GraphQLFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLInputObjectType, GraphQLString } from "graphql/type"


export default class ModelCreateInputGenerator extends ModelInputObjectTypeGenerator {
  /**
   * Generates an create model input field for a relational type, handling the four cases many/one and with/without related type. 
   * @param model 
   * @param field 
   * @param generators 
   */
  public static generateRelationFieldForInputType(model: IGQLType, field: IGQLField, generators: IGenerators) {
    const relationInfo = { relatedField: field, relatedType: model, relationName: field.relationName }
    const generator = ModelCreateInputGenerator.getGeneratorForRelation(model, field, generators)
    if(generator.wouldBeEmpty(field.type as IGQLType, relationInfo)) {
      return null
    } else {
      const schemaField = generator.generate(field.type as IGQLType, relationInfo)
      return generators.scalarTypeGenerator.requiredIf(field.isRequired, schemaField)
    }
  }

  public static relationWouldBeEmpty(model: IGQLType, field: IGQLField, generators: IGenerators) {
    const relationInfo = { relatedField: field, relatedType: model, relationName: field.relationName }
    const generator = ModelCreateInputGenerator.getGeneratorForRelation(model, field, generators)
    return generator.wouldBeEmpty(field.type as IGQLType, relationInfo)
  }

  public static getGeneratorForRelation(model: IGQLType, field: IGQLField, generators: IGenerators) : RelatedModelInputObjectTypeGenerator {
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
  public static generateScalarFieldTypeForInputType(model: IGQLType, field: IGQLField, generators: IGenerators) {
    if (field.isReadOnly) {
      return null
    } else {
      if (field.isList) {
        return generators.scalarListCreateInput.generate(model, field)
      } else {
        return generators.scalarTypeGenerator.mapToScalarFieldTypeForInput(field)
      }
    }
  }

  public wouldBeEmpty(model: IGQLType, args: {}) {
    return !this.hasWriteableFields(this.getScalarFields(model.fields)) &&
      this.getRelationFields(model.fields).every(field => ModelCreateInputGenerator.relationWouldBeEmpty(model, field, this.generators))
  }

  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}CreateInput`
  }


  protected generateRelationFieldType(model: IGQLType, args: {}, field: IGQLField) {
    return ModelCreateInputGenerator.generateRelationFieldForInputType(model, field, this.generators)
  }

  protected generateScalarFieldType(model: IGQLType, args: {}, field: IGQLField) {
    return ModelCreateInputGenerator.generateScalarFieldTypeForInputType(model, field, this.generators)
  }
}