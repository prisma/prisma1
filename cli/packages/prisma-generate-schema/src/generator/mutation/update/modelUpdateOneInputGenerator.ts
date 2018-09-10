import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"
import ModelUpdateInputGenerator from './modelUpdateInputGenerator'


export default class ModelUpdateOneInputTypeGenerator extends ModelUpdateInputGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    return `${input.name}UpdateOneInput`
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields)) {
      fields.create = { type: this.generators.modelCreateInput.generate(model, {}) }
      fields.update = { type: this.generators.modelUpdateDataInput.generate(model, {}) }
      fields.upsert = { type: this.generators.modelUpsertNestedInput.generate(model, {}) }
    }

    fields.delete = { type: this.generators.scalarTypeGenerator.generate('Boolean', {}) }

    // TODO: The disconnect field behavior of the scala implementation is very hard to replicate,
    // as it is not clear which order the types are traversed in. 
    // https://github.com/prisma/prisma/issues/3051
    if (!args.relatedField.isRequired || args.relatedField.isList) {
      fields.disconnect = { type: this.generators.scalarTypeGenerator.generate('Boolean', {}) }
    }

    if (!this.generators.modelWhereUniqueInput.wouldBeEmpty(model, args)) {
      fields.connect = { type: this.generators.modelWhereUniqueInput.generate(model, {}) }
    }

    return fields
  }
}