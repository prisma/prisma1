import { ISDL, DatabaseType, Renderer } from 'prisma-datamodel'
import { ModelInferrer } from '../../common/inferrer'

export abstract class DocumentInferrer extends ModelInferrer {

  protected model: ISDL

  constructor(model: ISDL, databaseType: DatabaseType, renderer?: Renderer) {
    super(databaseType, renderer)

    this.model = model
  }

  public async getSDL(): Promise<ISDL> {
    return this.model
  }
}