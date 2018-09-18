import { RelatedModelInputObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, TypeFromModelGenerator } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'

export default class ModelUpdateInputGenerator extends RelatedModelInputObjectTypeGenerator {
  public static generateScalarFieldTypeForInputType(model: IGQLType, field: IGQLField, generators: IGenerators) {
    if (TypeFromModelGenerator.reservedFields.includes(field.name)) {
      return null
    }

    if (field.isList) {
      return generators.scalarListUpdateInput.generate(model, field)
    } else {
      return generators.scalarTypeGenerator.mapToScalarFieldTypeForceOptional(field)
    }
  }
  /**
   * Generates an update model input field for a relational type, handling the four cases many/one and with/without related type. 
   * @param model 
   * @param field 
   * @param generators 
   */
  public static generateRelationFieldForInputType(model: IGQLType, field: IGQLField, generators: IGenerators) {
    if (field.relatedField !== null) {
      const relationInfo = { relatedField: field, relatedType: model, relationName: field.relationName }
      if (field.isList) {
        return generators.modelUpdateManyWithoutRelatedInput.generate(field.type as IGQLType, relationInfo)
      } else {
        return generators.modelUpdateOneWithoutRelatedInput.generate(field.type as IGQLType, relationInfo)
      }
    } else {
      const relationInfo = { relatedField: field, relatedType: model, relationName: null }
      if (field.isList) {
        return generators.modelUpdateManyInput.generate(field.type as IGQLType, relationInfo)
      } else {
        return generators.modelUpdateOneInput.generate(field.type as IGQLType, relationInfo)
      }
    }
  }

  public wouldBeEmpty(model: IGQLType, args: RelatedGeneratorArgs) {
    return !TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields)
  }

  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    return `${input.name}UpdateInput`
  }

  protected generateScalarFieldType(model: IGQLType, args: RelatedGeneratorArgs, field: IGQLField) {
    return ModelUpdateInputGenerator.generateScalarFieldTypeForInputType(model, field, this.generators)
  }

  protected generateRelationFieldType(model: IGQLType, args: RelatedGeneratorArgs, field: IGQLField) {
    return ModelUpdateInputGenerator.generateRelationFieldForInputType(model, field, this.generators)
  }
}