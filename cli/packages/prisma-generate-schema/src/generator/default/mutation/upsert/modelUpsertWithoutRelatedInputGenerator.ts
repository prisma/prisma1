import { GraphQLInputFieldConfigMap, GraphQLNonNull } from 'graphql/type'
import { IGQLField, IGQLType, capitalize } from 'prisma-datamodel'

import {
  RelatedGeneratorArgs,
  RelatedModelInputObjectTypeGenerator,
} from '../../../generator'

export default class ModelUpsertWithoutRelatedInputGenerator extends RelatedModelInputObjectTypeGenerator {
  public getTypeName(input: IGQLType, args: RelatedGeneratorArgs) {
    const field = args.relatedField.relatedField as IGQLField
    return `${input.name}UpsertWithout${capitalize(field.name)}Input`
  }

  protected generateWhereUnique(
    model: IGQLType,
    args: RelatedGeneratorArgs,
    fields: GraphQLInputFieldConfigMap,
  ) {
    // Do nothing - work is done in subclass
  }

  protected generateFields(model: IGQLType, args: RelatedGeneratorArgs) {
    const fields = {} as GraphQLInputFieldConfigMap

    this.generateWhereUnique(model, args, fields)

    if(!this.generators.modelUpdateWithoutRelatedDataInput.wouldBeEmpty(model, args)) {
      fields.update = {
        type: new GraphQLNonNull(
          this.generators.modelUpdateWithoutRelatedDataInput.generate(
            model,
            args,
          ),
        ),
      }
    }

    if(!this.generators.modelCreateWithoutRelatedInput.wouldBeEmpty(model, args)) {
      fields.create = {
        type: new GraphQLNonNull(
          this.generators.modelCreateWithoutRelatedInput.generate(model, args),
        ),
      }
    }

    return fields
  }
}
