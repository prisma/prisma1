import { Renderer, DatabaseType, ISDL, DefaultRenderer } from 'prisma-datamodel'
import ModelNameAndDirectiveNormalizer from './modelNameAndDirectiveNormalizer';

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

  public async getNormalizedDatamodel(baseModel: ISDL | null = null) : Promise<ISDL> {
    const model = await this.getDatamodel()
    new ModelNameAndDirectiveNormalizer(baseModel).normalize(model)
    return model
  }

  public async renderToNormalizedDatamodelString(baseModel: ISDL | null = null) : Promise<string> {
    return this.renderer.render(await this.getNormalizedDatamodel(baseModel))
  }

}