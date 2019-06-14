import { DatabaseType } from 'prisma-datamodel'
import { IntrospectionResult, DatabaseMetadata } from './introspectionResult'

export interface IConnector {
  listSchemas(): Promise<string[]>
  getMetadata(schemaName: string): Promise<DatabaseMetadata>
  introspect(schema: string): Promise<IntrospectionResult>
  getDatabaseType(): DatabaseType
}
