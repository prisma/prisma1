import { DocumentConnector } from "./documentConnector";


export interface IRelationInfo {
  sourceCollection: string
  targetCollection: string
  sourceKey: string
  targetKey: string
}

export interface IRelationResolver {
  resolve(resolver: DocumentConnector, samples: any[]) : IRelationInfo
}