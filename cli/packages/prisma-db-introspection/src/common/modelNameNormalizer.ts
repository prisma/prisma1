import { singular } from 'pluralize'
import { IGQLType, IGQLField, ISDL, capitalize, plural } from 'prisma-datamodel'

export default class ModelNameNormalizer {
  public normalize(model: ISDL) {
    for(const type of model.types) {
      this.normalizeType(type)
    } 
  }

  private setNameInternal(obj: IGQLType | IGQLField, newName: string) {
    if(obj.databaseName === undefined) {
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
      this.normalizeField(field, type)
    }
  }

  protected normalizeField(field: IGQLField, parentType: IGQLType) {
    // Make embedded type names pretty
    if(typeof field.type !== 'string' && field.type.isEmbedded) {
      // TODO: This might break with nested embedded types in incorrect order. 
      if(!field.type.databaseName)
        field.type.databaseName = field.type.name
      field.type.name = parentType.name + capitalize(singular(field.name))
    }
  }
}