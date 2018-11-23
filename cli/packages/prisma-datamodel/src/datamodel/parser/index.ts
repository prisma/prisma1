import { DatabaseType } from '../../databaseType'
import DocumentParser from './documentParser';
import RelationalParser from './relationalParser';
import Parser from './parser';

export default abstract class Parsers {
  public static create(databaseType: DatabaseType) : Parser {
    switch(databaseType) {
      case DatabaseType.mongo: return new DocumentParser()
      case DatabaseType.postgres: return new RelationalParser()
      default: throw new Error('Parser for database type not implemented: ' + databaseType)
    }
  }
}