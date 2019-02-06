import { IDirectiveInfo, RelationalParser, ISDL, IGQLField, IGQLType, DirectiveKeys, cloneType } from 'prisma-datamodel'
import { INormalizer } from './normalizer'

export class CopyEnums implements INormalizer {
  protected baseModel: ISDL

  constructor(baseModel: ISDL) {
    this.baseModel = baseModel
  }

  // Prisma does not store enums in the database,
  // so we copy all enums from the ref model, if no type with that name exists.
  public normalize(model: ISDL) {
    for(const refType of this.baseModel.types) {
      if(refType.isEnum === true && model.types.find(x => x.name === refType.name) === undefined) {
        model.types.push(cloneType(refType))
      }
    }
  }
}