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
    // Return a copy - object is muteable.
    // TODO: Add safe copy feature. JSON does not work as data struct has cycles. 
    return JSON.parse(JSON.stringify(this.model))
  }
}