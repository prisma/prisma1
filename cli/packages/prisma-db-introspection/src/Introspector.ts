import { PostgresConnector } from './connectors/PostgresConnector'
import { SdlPrinter } from './SdlPrinter'

export class Introspector {
  connectionString: string
  connector: PostgresConnector
  printer = new SdlPrinter()
  constructor(connectionString: string) {
    this.connectionString = connectionString
    this.connector = new PostgresConnector(connectionString)
  }

  async listSchemas(): Promise<string[]> {
    return this.connector.listSchemas()
  }

  async introspect(schemaName: string): Promise<string> {
    const tables = await this.connector.listTables(schemaName)

    const sdl = this.printer.print(tables)

    return sdl
  }
}
