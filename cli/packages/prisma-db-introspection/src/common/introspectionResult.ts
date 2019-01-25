import { Renderer, DatabaseType, ISDL, DefaultRenderer } from 'prisma-datamodel'
import ModelNameAndDirectiveNormalizer from './modelNameAndDirectiveNormalizer'
import ModelOrderNormalizer from './modelOrderNormalizer'

export abstract class IntrospectionResult {
  public renderer: Renderer
  public databaseType: DatabaseType
  
  constructor(databaseType: DatabaseType, renderer?: Renderer) {
    this.renderer = renderer || DefaultRenderer.create(databaseType)
    this.databaseType = databaseType
  }

  public abstract getDatamodel(): ISDL
  
  public renderToDatamodelString() : string {
    return this.renderer.render(this.getDatamodel())
  }

  public getNormalizedDatamodel(baseModel: ISDL | null = null) : ISDL {
    const model = this.getDatamodel()
    new ModelNameAndDirectiveNormalizer(baseModel).normalize(model)
    if(baseModel !== null) {
      new ModelOrderNormalizer(baseModel).normalize(model)
    }
    return model
  }

  public renderToNormalizedDatamodelString(baseModel: ISDL | null = null) : string {
    return this.renderer.render(this.getNormalizedDatamodel(baseModel))
  }

}