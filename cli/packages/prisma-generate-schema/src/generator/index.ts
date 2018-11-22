import { DatabaseType } from 'prisma-datamodel'
import RelationalGenerator  from './default'
import DocumentGenerator  from './document'
import { IGenerators } from './generator'


export default abstract class Generators {
  public static create(databaseType: DatabaseType) : IGenerators {
    switch(databaseType) {
      case DatabaseType.relational: return new RelationalGenerator()
      case DatabaseType.document: return new DocumentGenerator()
    }
  }
}