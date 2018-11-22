import { DatabaseType } from "prisma-datamodel";


export interface IConnector<IntrospectionType> {
  listSchemas(): Promise<string[]>
  listModels(schemaName: string): Promise<IntrospectionType>
  getDatabaseType(): DatabaseType
}