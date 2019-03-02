import { DatabaseType } from 'prisma-datamodel'
import { IntrospectionResult } from './introspectionResult'

export interface IConnector {
  listSchemas(): Promise<string[]>
  introspect(schema: string): Promise<IntrospectionResult>
  getDatabaseType(): DatabaseType
}
