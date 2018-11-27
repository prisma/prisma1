import { ISDL, DatabaseType, Renderer } from 'prisma-datamodel'
import { IntrospectionResult } from '../../common/introspectionResult'

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