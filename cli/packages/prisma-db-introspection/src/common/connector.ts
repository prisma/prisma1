
export interface IIntrospector<IntrospectionType> {
  listSchemas(): Promise<string[]>
  listModels(schemaName: string): Promise<IntrospectionType>
}