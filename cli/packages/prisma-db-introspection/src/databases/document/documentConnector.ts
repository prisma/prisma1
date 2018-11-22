import { IIntrospector } from '../../common/connector'
import { ISDL } from 'prisma-datamodel'


// Maybe we need a new type here in the future. 
export interface IDocumentConnector extends IIntrospector<ISDL> {
  listSchemas(): Promise<string[]>
  listModels(schemaName: string): Promise<ISDL>
}
