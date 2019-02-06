import { DatabaseType } from 'prisma-datamodel'
import { PostgresConnector } from './databases/relational/postgres/postgresConnector'
import { IConnector } from './common/connector'
import { MongoConnector } from './databases/document/mongo/mongoConnector'
import IDatabaseClient from './databases/IDatabaseClient'
import { MongoClient } from 'mongodb'
import { MysqlConnector } from './databases/relational/mysql/mysqlConnector'
import { Connection } from 'mysql'
import { Client } from 'pg'

export default abstract class Connectors {
  // TODO: find a proper type for databse config
  public static create(databaseType: DatabaseType, databaseClient: IDatabaseClient | MongoClient | Connection | Client ) : IConnector {
    switch(databaseType) {
      case DatabaseType.mongo: throw new MongoConnector(databaseClient as MongoClient)
      case DatabaseType.postgres: return new PostgresConnector(databaseClient as IDatabaseClient)
      case DatabaseType.mysql: return new MysqlConnector(databaseClient as IDatabaseClient)
      default: throw new Error('Not implemented.')
    }
  }
}