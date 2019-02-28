import { DatabaseType } from 'prisma-datamodel'
import RelationalGenerator from './default'
import DocumentGenerator from './document'
import { IGenerators } from './generator'

export default abstract class Generators {
  public static create(databaseType: DatabaseType): IGenerators {
    switch (databaseType) {
      case DatabaseType.postgres:
        return new RelationalGenerator()
      case DatabaseType.mongo:
        return new DocumentGenerator()
      default:
        throw new Error(
          'Schema generator for database type not implemented: ' + databaseType,
        )
    }
  }
}
