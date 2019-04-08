import {
  IDirectiveInfo,
  ISDL,
  IGQLField,
  IGQLType,
  DirectiveKeys,
} from 'prisma-datamodel'
import { INormalizer, Normalizer } from './normalizer'

export class AdjustJoinTableCardinality extends Normalizer {
  protected normalizeField(
    field: IGQLField,
    parentType: IGQLType,
    parentModel: ISDL,
  ) {
    if (
      typeof field.type === 'string' ||
      !field.isList ||
      this.baseField === null ||
      typeof this.baseField.type === 'string' ||
      this.baseField.type.name !== field.type.name
    ) {
      return
    }

    // If the reference is not a list. We restrict it to a non-list and add
    // a link: TABLE directive (compatability mode).
    if (!this.baseField.isList) {
      field.isList = false
      field.isRequired = this.baseField.isRequired

      const alreadyHasLinkDirective =
        field.relatedField &&
        field.relatedField.directives.some(
          d => d.name === DirectiveKeys.relation && Boolean(d.arguments.link),
        )

      if (alreadyHasLinkDirective) {
        return
      }
      field.directives.push(this.createLinkTableDirective(field))
    }
  }

  private createLinkTableDirective(field: IGQLField): IDirectiveInfo {
    // TODO: Find a way to do this without any
    const directive: any = {
      name: DirectiveKeys.relation,
      arguments: {
        link: 'TABLE',
      },
    }

    if (field.relationName) {
      directive.arguments.name = `"${field.relationName}"`
    }

    return directive
  }
}
