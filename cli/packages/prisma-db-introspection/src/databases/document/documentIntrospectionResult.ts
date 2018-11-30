import { ISDL, DatabaseType, Renderer } from 'prisma-datamodel'
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

  public async getDatamodel(): Promise<ISDL> {
    return this.model
  }
}