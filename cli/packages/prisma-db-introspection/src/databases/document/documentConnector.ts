import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType } from 'prisma-datamodel'


// Maybe we need a new type here in the future. 
export abstract class IDocumentConnector implements IConnector<ISDL> {
  getDatabaseType(): DatabaseType {
    return DatabaseType.document
  }
  abstract listSchemas(): Promise<string[]>
  abstract listModels(schemaName: string): Promise<ISDL>
}
