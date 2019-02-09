import { Renderer, DatabaseType, ISDL, DefaultRenderer } from 'prisma-datamodel'
import DefaultNormalizer from './normalization/defaultNormalizer'

export abstract class IntrospectionResult {
  public renderer: Renderer
  public databaseType: DatabaseType

  constructor(databaseType: DatabaseType, renderer?: Renderer) {
    this.renderer = renderer || DefaultRenderer.create(databaseType)
    this.databaseType = databaseType
  }

  public abstract getDatamodel(): ISDL

  public renderToDatamodelString(): string {
    return this.renderer.render(this.getDatamodel(), true)
  }

  public getNormalizedDatamodel(baseModel: ISDL | null = null): ISDL {
    const model = this.getDatamodel()

    DefaultNormalizer.create(this.databaseType, baseModel).normalize(model)

    return model
  }

  /**
   * Performs name normalization and order normalization.
   *
   * If a base model is given, order and additional directives are taken
   * from the base model.
   * @param baseModel
   */
  public renderToNormalizedDatamodelString(baseModel: ISDL | null = null): string {
    return this.renderer.render(this.getNormalizedDatamodel(baseModel))
  }
}
