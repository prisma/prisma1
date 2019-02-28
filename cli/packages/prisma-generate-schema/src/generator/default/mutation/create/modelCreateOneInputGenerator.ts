import { GraphQLInputObjectType } from 'graphql/type'
import { IGQLType } from 'prisma-datamodel'
import { RelatedGeneratorArgs } from '../../../generator'
import { ModelCreateOneOrManyInputGenerator } from './modelCreateManyInputGenerator'

export default class ModelCreateOneInputGenerator extends ModelCreateOneOrManyInputGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    return `${input.name}CreateOneInput`
  }
  protected maybeWrapList(input: GraphQLInputObjectType) {
    return input
  }
}
