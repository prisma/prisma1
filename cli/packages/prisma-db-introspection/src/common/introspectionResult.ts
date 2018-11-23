import { Renderer, DatabaseType, ISDL, DefaultRenderer } from 'prisma-datamodel'

export abstract class IntrospectionResult {
  public renderer: Renderer
  public databaseType: DatabaseType
  
  constructor(databaseType: DatabaseType, renderer?: Renderer) {
    this.renderer = renderer || DefaultRenderer.create(databaseType)
    this.databaseType = databaseType
  }

  public abstract getDatamodel() : Promise<ISDL>
  
  public async renderToDatamodelString() : Promise<string> {
    return this.renderer.render(await this.getDatamodel())
  }
}