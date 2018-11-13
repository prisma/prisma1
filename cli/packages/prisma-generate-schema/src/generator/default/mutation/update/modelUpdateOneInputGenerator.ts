import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../../generator'
import { IGQLType, IGQLField } from '../../../../datamodel/model'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"
import ModelUpdateInputGenerator from './modelUpdateInputGenerator'


export default class ModelUpdateOneInputTypeGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    return `${input.name}UpdateOneInput`
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (this.hasWriteableFields(model.fields)) {
      fields.create = { type: this.generators.modelCreateInput.generate(model, {}) }
      fields.update = { type: this.generators.modelUpdateDataInput.generate(model, {}) }
      fields.upsert = { type: this.generators.modelUpsertNestedInput.generate(model, {}) }
    }

    fields.delete = { type: this.generators.scalarTypeGenerator.generate('Boolean', {}) }
    
    fields.disconnect = { type: this.generators.scalarTypeGenerator.generate('Boolean', {}) }

    if (!this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)) {
      fields.connect = { type: this.generators.modelWhereUniqueInput.generate(model, {}) }
    }

    return fields
  }
}