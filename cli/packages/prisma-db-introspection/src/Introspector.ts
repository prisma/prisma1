import { PostgresConnector } from './connectors/PostgresConnector'
import { SdlPrinter } from './SdlPrinter'
import { ClientConfig } from 'pg'
import { PostgresConnectionDetails } from './types/common'

export class Introspector {
  connectionString: string | ClientConfig
  connector: PostgresConnector
  printer = new SdlPrinter()
  constructor(connectionString: PostgresConnectionDetails) {
    this.connectionString = connectionString
    this.connector = new PostgresConnector(connectionString)
  }

  async listSchemas(): Promise<string[]> {
    return this.connector.listSchemas()
  }

  async introspect(
    schemaName: string,
  ): Promise<{ numTables: number; sdl: string }> {
    const tables = await this.connector.listTables(schemaName)

    const sdl = this.printer.print(tables)

    return { numTables: tables.length, sdl }
  }
}
