import { Renderer, DatabaseType, ISDL, DefaultRenderer } from 'prisma-datamodel'

export abstract class ModelInferrer {
  protected renderer: Renderer
  protected databaseType: DatabaseType
  
  constructor(databaseType: DatabaseType, renderer?: Renderer) {
    this.renderer = renderer || DefaultRenderer.create(databaseType)
    this.databaseType = databaseType
  }

  public abstract getSDL() : Promise<ISDL>

  public async renderToDatamodelString() : Promise<string> {
    return this.renderer.render(await this.getSDL())
  }
}