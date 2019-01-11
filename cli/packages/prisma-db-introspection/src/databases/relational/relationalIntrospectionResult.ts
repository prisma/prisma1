import { IntrospectionResult } from "../../common/introspectionResult"
import { Table } from "./relationalConnector"
import { ISDL, DatabaseType, Renderer } from 'prisma-datamodel'

export abstract class RelationalIntrospectionResult extends IntrospectionResult {

  protected model: Table[]

  constructor(model: Table[], databaseType: DatabaseType, renderer?: Renderer) {
    super(databaseType, renderer)

    this.model = model
  }

  public async getDatamodel(): Promise<ISDL> {
    return await this.infer(this.model)
  }
  
  abstract async infer(model: Table[]): Promise<ISDL> 
}