export interface RC {
  cloudSessionKey?: string
  clusters?: Clusters
}

export interface Clusters {
  [name: string]: ClusterConfig
}

export interface ClusterConfig {
  host: string
  clusterSecret: string
}

export interface Header {
  name: string
  value: string
}

export interface FunctionInput {
  name: string
  query: string
  url: string
  headers: Header[]
}
