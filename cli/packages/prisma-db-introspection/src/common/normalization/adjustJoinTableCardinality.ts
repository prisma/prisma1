import {
  IDirectiveInfo,
  ISDL,
  IGQLField,
  IGQLType,
  DirectiveKeys,
} from 'prisma-datamodel'
import { INormalizer } from './normalizer'

export class AdjustJoinTableCardinality implements INormalizer {
  protected baseModel: ISDL

  constructor(baseModel: ISDL) {
    this.baseModel = baseModel
  }

  public normalizeType(type: IGQLType, ref: IGQLType) {
    for (const field of type.fields) {
      // Only look at n:n relations.
      if (typeof field.type !== 'string' && field.isList) {
        // TODO: Subclass
        // Fid the reference field.
        const refField = ref.fields.find(x => x.name === field.name)

        if (refField === undefined || typeof refField.type === 'string')
          continue

        if (refField.type.name !== field.type.name) continue

        // If the reference is not a list. We restrict it to a non-list and add
        // a link: TABLE directive (compatability mode).
        if (!refField.isList) {
          field.isList = false
          field.isRequired = refField.isRequired
          console.log(`Going to push linkTableDirective`)
          console.log(field.directives)
          field.directives.push(this.createLinkTableDirective(field))
          if (field.relatedField !== null) {
            field.relatedField.directives.push(
              this.createLinkTableDirective(field.relatedField),
            )
          }
        }
      }
    }
  }

  public normalize(model: ISDL) {
    for (const type of model.types) {
      // TODO: We should move all tooling for finding types or fields into some common class.
      const ref = this.baseModel.types.find(
        x => x.name === type.name || x.databaseName === type.name,
      )
      if (ref !== undefined) {
        this.normalizeType(type, ref)
      }
    }
  }

  private createLinkTableDirective(field: IGQLField): IDirectiveInfo {
    const directive = {
      name: DirectiveKeys.relation,
      arguments: {
        link: 'TABLE',
      },
    }

    if (field.relationName) {
      directive.name = field.relationName
    }

    return directive
  }
}
