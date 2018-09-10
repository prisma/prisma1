import { ArgumentsGenerator, RelatedGeneratorArgs } from "../generator"
import { IGQLField, IGQLType } from "../../datamodel/model"
import { GraphQLFieldConfigArgumentMap, GraphQLInt, GraphQLString } from 'graphql/type'

export default class OneQueryArgumentsGenerator extends ArgumentsGenerator {
  public generate(model: IGQLType, args: {}) {
    return {
      where: { type: this.getGenerator().generate(model, {}) }
    } as GraphQLFieldConfigArgumentMap
  }

  public wouldBeEmpty(model: IGQLType, args: {}) {
    return this.getGenerator().wouldBeEmpty(model, {})
  }

  private getGenerator() {
    return this.generators.modelWhereInput
  }

}