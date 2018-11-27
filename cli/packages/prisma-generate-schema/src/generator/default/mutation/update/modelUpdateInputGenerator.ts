import { RelatedModelInputObjectTypeGenerator, RelatedGeneratorArgs, IGenerators, TypeFromModelGenerator } from '../../../generator'
import { IGQLType, IGQLField } from 'prisma-datamodel'

export default class ModelUpdateInputGenerator extends RelatedModelInputObjectTypeGenerator {
  public static generateScalarFieldTypeForInputType(model: IGQLType, field: IGQLField, generators: IGenerators) {
    if (field.isReadOnly) {
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
    const relationInfo = { relatedField: field, relatedType: model, relationName: field.relationName }
    const generator = ModelUpdateInputGenerator.getGeneratorForRelation(model, field, generators)
    if(generator.wouldBeEmpty(field.type as IGQLType, relationInfo)) {
      return null
    } else {
      return generator.generate(field.type as IGQLType, relationInfo)
    }
  }
  public static relationWouldBeEmpty(model: IGQLType, field: IGQLField, generators: IGenerators) {
    const relationInfo = { relatedField: field, relatedType: model, relationName: field.relationName }
    const generator = ModelUpdateInputGenerator.getGeneratorForRelation(model, field, generators)
    return generator.wouldBeEmpty(field.type as IGQLType, relationInfo)
  }

  public static getGeneratorForRelation(model: IGQLType, field: IGQLField, generators: IGenerators) : RelatedModelInputObjectTypeGenerator{
    if (field.relatedField !== null) {
      if (field.isList) {
        return generators.modelUpdateManyWithoutRelatedInput
      } else {
        if (field.isRequired) {
          return generators.modelUpdateOneRequiredWithoutRelatedInput
        } else {
          return generators.modelUpdateOneWithoutRelatedInput
        }
      }
    } else {
      if (field.isList) {
        return generators.modelUpdateManyInput
      } else {
        if (field.isRequired) {
          return generators.modelUpdateOneRequiredInput
        } else {
          return generators.modelUpdateOneInput
        }
      }
    }
  }

  public wouldBeEmpty(model: IGQLType, args: RelatedGeneratorArgs) {  
    return !this.hasWriteableFields(model.fields)
    // TODO: We should add the following check, but this requires
    // a caching mechanism to avoid recursive checks.   
    // this.getRelationFields(model.fields).every(field => ModelUpdateInputGenerator.relationWouldBeEmpty(model, field, this.generators))
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