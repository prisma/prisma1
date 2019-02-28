import {
  ModelInputObjectTypeGenerator,
  RelatedGeneratorArgs,
  IGenerators,
  FieldConfigUtils,
} from '../../generator'
import { IGQLType, IGQLField } from 'prisma-datamodel'
import {
  GraphQLObjectType,
  GraphQLInputFieldConfigMap,
  GraphQLFieldConfig,
  GraphQLList,
  GraphQLNonNull,
  GraphQLInputObjectType,
  GraphQLString,
} from 'graphql/type'
import ModelWhereInputGenerator from '../query/modelWhereInputGenerator'

export default class ModelSubscriptionWhereInput extends ModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: {}) {
    return `${input.name}SubscriptionWhereInput`
  }

  protected getLogicalOperators(): string[] {
    return ['AND', 'OR', 'NOT']
  }

  protected generateFields(
    model: IGQLType,
    args: {},
  ): GraphQLInputFieldConfigMap {
    let fields = {
      mutation_in: {
        type: new GraphQLList(
          new GraphQLNonNull(this.generators.mutationType.generate(null, {})),
        ),
      },
      updatedFields_contains: { type: GraphQLString },
      updatedFields_contains_every: {
        type: new GraphQLList(new GraphQLNonNull(GraphQLString)),
      },
      updatedFields_contains_some: {
        type: new GraphQLList(new GraphQLNonNull(GraphQLString)),
      },
      node: { type: this.generators.modelWhereInput.generate(model, {}) },
    } as GraphQLInputFieldConfigMap

    const recursiveFilter = ModelWhereInputGenerator.generateFiltersForSuffix(
      this.getLogicalOperators(),
      null,
      this.generators.scalarTypeGenerator.wrapList(
        this.generators.modelSubscriptionWhereInput.generate(model, {}),
      ),
    )

    fields = FieldConfigUtils.merge(fields, recursiveFilter)

    return fields
  }
}
