import { singular } from 'pluralize'
import { IGQLType, IGQLField, ISDL, capitalize, plural, toposort } from 'prisma-datamodel'
import { INormalizer } from './normalizer';

export default class ModelNameNormalizer implements INormalizer {
  public normalize(model: ISDL) {
    // We need to sort types according to topological order for name normalization.
    // Otherwise embedded type naming might break as embedded types depend on 
    // their parent type. 
    for(const type of toposort(model.types)) {
      this.normalizeType(type, model)
    } 
  }

  protected assignName(obj: IGQLType | IGQLField, newName: string) {
    if(obj.databaseName === null) {
      // If name was already changed, we don't touch it.
      if(newName !== obj.name) {
        // If name is already conforming to prisma, skip. 
        obj.databaseName = obj.name
        obj.name = newName
      }
    }
  }

  protected normalizeType(type: IGQLType, parentModel: ISDL, forceNoRename: boolean = false) {
    
    if(!forceNoRename) {
      this.assignName(type, capitalize(singular(type.name)))
    }

    for(const field of type.fields) {
      this.normalizeField(field, type, parentModel)
    }
  }

  protected normalizeField(field: IGQLField, parentType: IGQLType, parentModel: ISDL) {
    // Make embedded type names pretty
    if(typeof field.type !== 'string' && field.type.isEmbedded) {
      if(!field.type.databaseName)
        field.type.databaseName = field.type.name

      field.type.name = parentType.name + capitalize(singular(field.name))
    }
  }
}