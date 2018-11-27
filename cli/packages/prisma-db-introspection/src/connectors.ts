import { DatabaseType } from 'prisma-datamodel'
import { PostgresConnector } from './databases/relational/postgres/postgresConnector'
import { IConnector } from './common/connector'

export default abstract class Connectors {
  // TODO: find a proper type for databse config
  public static create(databaseType: DatabaseType, databaseConfig: any) : IConnector {
    switch(databaseType) {
      case DatabaseType.mongo: throw new Error('Not implemented')
      case DatabaseType.postgres: return new PostgresConnector(databaseConfig)
      default: throw new Error('Not implemented.')
    }
  }
}