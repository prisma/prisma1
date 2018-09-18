import { ModelObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, ModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'
import { GraphQLObjectType, GraphQLFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLInputObjectType, GraphQLString } from "graphql/type"


export default class ModelCreateInputGenerator extends ModelInputObjectTypeGenerator {


  /**
   * Generates an create model input field for a relational type, handling the four cases many/one and with/without related type. 
   * @param model 
   * @param field 
   * @param generators 
   */
  public static generateRelationFieldForInputType(model: IGQLType, field: IGQLField, generators: IGenerators) {
    if (field.relatedField !== null) {
      const relationInfo = { relatedField: field, relatedType: model, relationName: field.relationName }
      if (field.isList) {
        return generators.scalarTypeGenerator.requiredIf(field.isRequired, generators.modelCreateManyWithoutRelatedInput.generate(field.type as IGQLType, relationInfo))
      } else {
        return generators.scalarTypeGenerator.requiredIf(field.isRequired, generators.modelCreateOneWithoutRelatedInput.generate(field.type as IGQLType, relationInfo))
      }
    } else {
      const relationInfo = { relatedField: field, relatedType: model, relationName: null }
      if (field.isList) {
        return generators.scalarTypeGenerator.requiredIf(field.isRequired, generators.modelCreateManyInput.generate(field.type as IGQLType, relationInfo))
      } else {
        return generators.scalarTypeGenerator.requiredIf(field.isRequired, generators.modelCreateOneInput.generate(field.type as IGQLType, relationInfo))
      }
    }
  }

  public static generateScalarFieldTypeForInputType(model: IGQLType, field: IGQLField, generators: IGenerators) {
    if (TypeFromModelGenerator.reservedFields.includes(field.name)) {
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
    return !TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields)
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