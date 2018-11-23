import { IConnector } from '../../common/connector'
import { ISDL, DatabaseType } from 'prisma-datamodel'
import { ModelInferrer } from '../../common/inferrer'
import { DocumentInferrer } from './documentInferrer';


// Maybe we need a new type here in the future. 
export abstract class DocumentConnector implements IConnector {
  getDatabaseType(): DatabaseType {
    return DatabaseType.document
  }
  abstract listSchemas(): Promise<string[]>
  abstract listModels(schemaName: string): Promise<ISDL>
  abstract inferrer(schema: string): Promise<DocumentInferrer> 
}
