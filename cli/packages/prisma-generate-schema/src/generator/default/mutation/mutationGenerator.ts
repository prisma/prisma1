import { RootGenerator, FieldConfigUtils } from '../../generator'
import { IGQLType } from '../../../datamodel/model'
import {
  GraphQLObjectType,
  GraphQLID,
  GraphQLFieldConfigMap,
  GraphQLFieldConfig,
  GraphQLList,
  GraphQLNonNull,
  GraphQLInputObjectType,
  GraphQLString,
} from 'graphql/type'
import { plural, camelCase } from '../../../util/util'

export default class MutationGenerator extends RootGenerator {
  public getTypeName(input: IGQLType[], args: {}) {
    return 'Mutation'
  }
  protected generateInternal(input: IGQLType[], args: {}) {
    const fieldMaps = input
      .filter(type => !type.isEnum && !type.isEmbedded)
      .map(type =>
        FieldConfigUtils.merge(
          this.generateCreateField(type),
          this.generateUpdateField(type),
          this.generateUpdateManyField(type),
          this.generateUpsertField(type),
          this.generateDeleteField(type),
          this.generateDeleteManyField(type),
        ),
      )

    return new GraphQLObjectType({
      name: this.getTypeName(input, args),
      fields: FieldConfigUtils.merge(...fieldMaps),
    })
  }

  private generateCreateField(model: IGQLType) {
    const fields = {} as GraphQLFieldConfigMap<null, null>

    // TODO: model.fields.length === 0 should be encapuslated in the respective "wouldBeEmpty" or another helper function
    const wouldBeEmpty =
      model.fields.length === 0 &&
      this.generators.modelCreateInput.wouldBeEmpty(model, {})
    const nonIdFields = model.fields.filter(field => field.name !== 'id')

    if (wouldBeEmpty) {
      return fields
    }

    fields[`create${model.name}`] = {
      type: new GraphQLNonNull(this.generators.model.generate(model, {})),
      args:
        nonIdFields.length === 0
          ? {}
          : {
              data: {
                type: new GraphQLNonNull(
                  this.generators.modelCreateInput.generate(model, {}),
                ),
              },
            },
    }

    return fields
  }

  private generateUpdateField(model: IGQLType) {
    const fields = {} as GraphQLFieldConfigMap<null, null>

    if (this.generators.modelUpdateInput.wouldBeEmpty(model, {})) {
      return fields
    }

    if (this.generators.modelWhereUniqueInput.wouldBeEmpty(model, {})) {
      return fields
    }

    fields[`update${model.name}`] = {
      type: this.generators.model.generate(model, {}),
      args: {
        data: {
          type: new GraphQLNonNull(
            this.generators.modelUpdateInput.generate(model, {}),
          ),
        },
        where: {
          type: new GraphQLNonNull(
            this.generators.modelWhereUniqueInput.generate(model, {}),
          ),
        },
      },
    }

    return fields
  }

  private generateUpdateManyField(model: IGQLType) {
    const fields = {} as GraphQLFieldConfigMap<null, null>

    if (this.generators.modelUpdateInput.wouldBeEmpty(model, {})) {
      return fields
    }
    if (this.generators.modelWhereInput.wouldBeEmpty(model, {})) {
      return fields
    }

    fields[`updateMany${plural(model.name)}`] = {
      type: new GraphQLNonNull(this.generators.batchPayload.generate(null, {})),
      args: {
        data: {
          type: new GraphQLNonNull(
            this.generators.modelUpdateManyMutationInput.generate(model, {}),
          ),
        },
        where: { type: this.generators.modelWhereInput.generate(model, {}) },
      },
    }

    return fields
  }

  private generateUpsertField(model: IGQLType) {
    const fields = {} as GraphQLFieldConfigMap<null, null>

    if (this.generators.modelCreateInput.wouldBeEmpty(model, {})) {
      return fields
    }
    if (this.generators.modelUpdateInput.wouldBeEmpty(model, {})) {
      return fields
    }
    if (this.generators.modelWhereUniqueInput.wouldBeEmpty(model, {})) {
      return fields
    }

    fields[`upsert${model.name}`] = {
      type: new GraphQLNonNull(this.generators.model.generate(model, {})),
      args: {
        where: {
          type: new GraphQLNonNull(
            this.generators.modelWhereUniqueInput.generate(model, {}),
          ),
        },
        create: {
          type: new GraphQLNonNull(
            this.generators.modelCreateInput.generate(model, {}),
          ),
        },
        update: {
          type: new GraphQLNonNull(
            this.generators.modelUpdateInput.generate(model, {}),
          ),
        },
      },
    }

    return fields
  }

  private generateDeleteField(model: IGQLType) {
    const fields = {} as GraphQLFieldConfigMap<null, null>

    if (this.generators.modelWhereUniqueInput.wouldBeEmpty(model, {})) {
      return fields
    }

    fields[`delete${model.name}`] = {
      type: this.generators.model.generate(model, {}),
      args: {
        where: {
          type: new GraphQLNonNull(
            this.generators.modelWhereUniqueInput.generate(model, {}),
          ),
        },
      },
    }

    return fields
  }

  private generateDeleteManyField(model: IGQLType) {
    const fields = {} as GraphQLFieldConfigMap<null, null>

    if (this.generators.modelWhereInput.wouldBeEmpty(model, {})) {
      return fields
    }

    fields[`deleteMany${plural(model.name)}`] = {
      type: new GraphQLNonNull(this.generators.batchPayload.generate(null, {})),
      args: {
        where: { type: this.generators.modelWhereInput.generate(model, {}) },
      },
    }

    return fields
  }
}
