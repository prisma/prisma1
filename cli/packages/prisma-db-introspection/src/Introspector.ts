import { PostgresConnector } from './connectors/PostgresConnector'
import { SDLInferrer } from './SDLInferrer'
import { ClientConfig } from 'pg'
import { Connector } from './types/common'

export class Introspector {
  connector: Connector
  inferrer: SDLInferrer

  constructor(connector: Connector) {
    this.connector = connector
    this.inferrer = new SDLInferrer()
  }

  async listSchemas(): Promise<string[]> {
    return this.connector.listSchemas()
  }

  async introspect(
    schemaName: string,
  ): Promise<{ numTables: number; sdl: string }> {
    const dbTables = await this.connector.listTables(schemaName)
    const sdl = this.inferrer.infer(dbTables)
    return { numTables: dbTables.length, sdl: sdl.render() }
  }
}
