import Renderer from './renderer'
import DocumentRenderer from './documentRenderer'
import RelationalRenderer from './relationalRenderer'
import RelationalRendererV2 from './relationalRendererV2'
import { DatabaseType } from '../../databaseType'
import GQLAssert from '../../util/gqlAssert'

export default abstract class Renderers {
  public static create(
    databaseType: DatabaseType,
    enableBeta: boolean = false,
  ): Renderer {
    if (enableBeta) {
      switch (databaseType) {
        case DatabaseType.mongo:
          return new DocumentRenderer()
        case DatabaseType.mysql:
          return new RelationalRendererV2()
        case DatabaseType.postgres:
          return new RelationalRendererV2()
        case DatabaseType.sqlite:
          return new RelationalRendererV2()
      }
    } else {
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
    }

    GQLAssert.raise(
      `Attempting to create renderer for unknown database type: ${databaseType}`,
    )
    return new DocumentRenderer() // Make TS happy.
  }
}
