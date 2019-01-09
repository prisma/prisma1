import { ArgumentsGenerator } from "../../generator"
import { IGQLType } from 'prisma-datamodel'
import { GraphQLFieldConfigArgumentMap, GraphQLNonNull } from 'graphql/type'

export default class UniqueQueryArgumentsGenerator extends ArgumentsGenerator {
  public wouldBeEmpty(model: IGQLType, args: {}) {
    return this.getGenerator().wouldBeEmpty(model, {})
  }

  public generate(model: IGQLType, args: {}) {
    return {
      where: { type: new GraphQLNonNull(this.getGenerator().generate(model, {})) }
    } as GraphQLFieldConfigArgumentMap
  }

  private getGenerator() {
    return this.generators.modelWhereUniqueInput
  }
}