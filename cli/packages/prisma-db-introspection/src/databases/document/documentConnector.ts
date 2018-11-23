import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType } from 'prisma-datamodel'
import { DocumentIntrospectionResult } from './documentIntrospectionResult'

// Maybe we need a new type here in the future. 
export abstract class DocumentConnector implements IConnector {
  getDatabaseType(): DatabaseType {
    return DatabaseType.document
  }
  abstract listSchemas(): Promise<string[]>
  abstract listModels(schemaName: string): Promise<ISDL>
  abstract introspect(schema: string): Promise<DocumentIntrospectionResult> 
}
