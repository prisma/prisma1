import { DatabaseType } from "prisma-datamodel";
import { ModelInferrer } from "./inferrer";


export interface IConnector {
  listSchemas(): Promise<string[]>
  inferrer(schema: string): Promise<ModelInferrer>
  getDatabaseType(): DatabaseType
}