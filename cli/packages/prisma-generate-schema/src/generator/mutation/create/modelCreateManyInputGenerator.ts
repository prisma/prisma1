import { ModelObjectTypeGenerator, RelatedGeneratorArgs, RelatedModelInputObjectTypeGenerator, ModelInputObjectTypeGenerator, TypeFromModelGenerator } from '../../generator'
import { IGQLType, IGQLField } from '../../../datamodel/model'
import { GraphQLObjectType, GraphQLInputFieldConfigMap, GraphQLFieldConfig, GraphQLList, GraphQLNonNull, GraphQLInputObjectType, GraphQLString } from "graphql/type"

export abstract class ModelCreateOneOrManyInputGenerator extends ModelInputObjectTypeGenerator {

  public wouldBeEmpty(model: IGQLType, args: {}) {
    return !TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields) &&
      !TypeFromModelGenerator.hasUniqueField(model.fields)
  }

  protected abstract maybeWrapList(input: GraphQLInputObjectType): GraphQLList<GraphQLNonNull<GraphQLInputObjectType>> | GraphQLInputObjectType

  protected generateFields(model: IGQLType, args: {}) {
    const fields = {} as GraphQLInputFieldConfigMap

    if (TypeFromModelGenerator.hasFieldsExcept(model.fields, ...TypeFromModelGenerator.reservedFields)) {
      fields.create = { type: this.maybeWrapList(this.generators.modelCreateInput.generate(model, {})) }
    }

    if (TypeFromModelGenerator.hasUniqueField(model.fields)) {
      fields.connect = { type: this.maybeWrapList(this.generators.modelWhereUniqueInput.generate(model, {})) }
    }

    return fields
  }
}

// tslint:disable-next-line:max-classes-per-file
export default class ModelCreateManyInputGenerator extends ModelCreateOneOrManyInputGenerator {

  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    return `${input.name}CreateManyInput`
  }

  protected maybeWrapList(input: GraphQLInputObjectType): GraphQLList<GraphQLNonNull<GraphQLInputObjectType>> | GraphQLInputObjectType {
    return this.generators.scalarTypeGenerator.wrapList(input)
  }
}