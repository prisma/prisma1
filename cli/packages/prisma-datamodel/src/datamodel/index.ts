import { DatabaseType } from '../databaseType'
import DocumentParser from './documentParser';
import RelationalParser from './relationalParser';
import Parser from './parser';

export default abstract class Parsers {
  public static create(databaseType: DatabaseType) : Parser {
    switch(databaseType) {
      case DatabaseType.document: return new DocumentParser()
      case DatabaseType.relational: return new RelationalParser()
    }
  }
}