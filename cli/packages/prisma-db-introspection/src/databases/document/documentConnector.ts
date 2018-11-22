import { IIntrospector } from "../../common/connector"

export interface IDocumentConnector extends IIntrospector<TODO> {
  listSchemas(): Promise<string[]>
  listModels(schemaName: string): Promise<TODO>
}
