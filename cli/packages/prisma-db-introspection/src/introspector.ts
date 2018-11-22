import { ClientConfig, Client } from 'pg'
import { IConnector } from './common/connector'
import { IInferrer } from './common/inferrer';
import { Renderer, DatabaseType } from 'prisma-datamodel'
import { PostgresConnector } from './databases/relational/postgres/postgresConnector';
import { PostgresInferrer } from './databases/relational/postgres/postgresInferrer';

export class ModelInferrer<InternalType> {
  connector: IConnector<InternalType>
  inferrer: IInferrer<InternalType>

  constructor(connector: IConnector<InternalType>, inferrer: IInferrer<InternalType>) {
    this.connector = connector
    this.inferrer = inferrer
  }

  async listSchemas(): Promise<string[]> {
    return this.connector.listSchemas()
  }

  async introspect(schemaName: string) {
    const models = await this.connector.listModels(schemaName)
    const sdl = await this.inferrer.infer(models)
    const rendered = Renderer.create(this.connector.getDatabaseType()).render(sdl)

    return { modelCount: sdl.types.length, rendered }
  }
}

export abstract class Introspector {
  public static create(databaseType: DatabaseType, client: Client) {
    switch(databaseType) {
      default: return new ModelInferrer(new PostgresConnector(client), new PostgresInferrer())
    }
  }
}