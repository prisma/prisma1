import { ISDL, DatabaseType, Renderer, cloneSchema } from 'prisma-datamodel'
import { IntrospectionResult } from '../../common/introspectionResult'

/**
 * Simple wrapper class for an introspection result to keep interface
 * compatibility with relational connectors.
 */
export class DocumentIntrospectionResult extends IntrospectionResult {
  protected model: ISDL

  constructor(model: ISDL, databaseType: DatabaseType, renderer?: Renderer) {
    super(databaseType, renderer)

    this.model = model
  }

  /**
   * @deprecated This returns an unnormalized datamodel and might get removed in the near future.
   */
  public getDatamodel(): ISDL {
    // Return a copy - object is muteable.
    return cloneSchema(this.model)
  }
}
