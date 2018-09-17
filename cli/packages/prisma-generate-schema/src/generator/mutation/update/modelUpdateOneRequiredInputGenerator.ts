import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"
import ModelUpdateInputGenerator from './modelUpdateInputGenerator'


export default class ModelUpdateOneRequiredInputTypeGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    return `${input.name}UpdateOneRequiredInput`
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields)) {
      fields.create = { type: this.generators.modelCreateInput.generate(model, {}) }
      fields.update = { type: this.generators.modelUpdateDataInput.generate(model, {}) }
      fields.upsert = { type: this.generators.modelUpsertNestedInput.generate(model, {}) }
    }

    return fields
  }
}