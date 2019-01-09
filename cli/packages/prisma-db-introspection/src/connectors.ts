import { DatabaseType } from 'prisma-datamodel'
import { PostgresConnector } from './databases/relational/postgres/postgresConnector'
import { IConnector } from './common/connector'
import { MongoConnector } from './databases/document/mongo/mongoConnector';

export default abstract class Connectors {
  // TODO: find a proper type for databse config
  public static create(databaseType: DatabaseType, databaseConfig: any) : IConnector {
    switch(databaseType) {
      case DatabaseType.mongo: throw new MongoConnector(databaseConfig)
      case DatabaseType.postgres: return new PostgresConnector(databaseConfig)
      default: throw new Error('Not implemented.')
    }
  }
}