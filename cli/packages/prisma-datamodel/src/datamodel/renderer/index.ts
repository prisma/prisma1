import Renderer from './renderer'
import DocumentRenderer from './documentRenderer'
import LegacyRelationalRenderer from './legacyRelationalRenderer'
import RelationalRenderer from './relationalRenderer'
import { DatabaseType } from '../../databaseType'
import GQLAssert from '../../util/gqlAssert'

export default abstract class DefaultRenderer {
  public static create(databaseType: DatabaseType, enableV2: boolean = false): Renderer {
    if (enableV2) {
      switch (databaseType) {
        case DatabaseType.mongo:
          return new DocumentRenderer()
        case DatabaseType.mysql:
          return new RelationalRenderer()
        case DatabaseType.postgres:
          return new RelationalRenderer()
        case DatabaseType.sqlite:
          return new RelationalRenderer()
      }
    } else {
      switch (databaseType) {
        case DatabaseType.mongo:
          return new DocumentRenderer()
        case DatabaseType.mysql:
          return new LegacyRelationalRenderer()
        case DatabaseType.postgres:
          return new LegacyRelationalRenderer()
        case DatabaseType.sqlite:
          return new LegacyRelationalRenderer()
      }
    }

    GQLAssert.raise(`Attempting to create renderer for unknown database type: ${databaseType}`)
    return new DocumentRenderer() // Make TS happy.
  }
}
