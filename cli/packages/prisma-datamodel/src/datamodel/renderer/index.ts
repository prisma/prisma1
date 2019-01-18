import Renderer from './renderer'
import DocumentRenderer from './documentRenderer'
import RelationalRenderer from './relationalRenderer'
import { DatabaseType } from '../../databaseType'

export default abstract class Renderers {
  public static create(databaseType: DatabaseType) : Renderer {
    switch(databaseType) {
      case DatabaseType.mongo: return new DocumentRenderer()
      case DatabaseType.mysql: return new RelationalRenderer()
      case DatabaseType.postgres: return new RelationalRenderer()
      case DatabaseType.sqlite: return new RelationalRenderer()
    }
  }
}