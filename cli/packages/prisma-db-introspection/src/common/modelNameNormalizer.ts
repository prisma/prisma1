import { singular } from 'pluralize'
import { IGQLType, IGQLField, ISDL, capitalize, plural } from 'prisma-datamodel'

export default class ModelNameNormalizer {
  public normalize(model: ISDL) {
    for(const type of model.types) {
      this.normalizeType(type)
    } 
  }

  private setNameInternal(obj: IGQLType | IGQLField, newName: string) {
    if(obj.databaseName !== undefined) {
      // If name was already changed, we don't touch it.
      if(newName !== obj.name) {
        // If name is already conforming to prisma, skip. 
        obj.databaseName = obj.name
        obj.name = newName
      }
    }
  }

  protected normalizeType(type: IGQLType) {
    this.setNameInternal(type, capitalize(singular(type.name)))

    for(const field of type.fields) {
      this.normalizeField(field)
    }
  }

  protected normalizeField(field: IGQLField) {
    this.setNameInternal(field, plural(field.name))
  }
}