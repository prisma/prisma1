export interface RC {
  'graphcool-framework'?: FrameworkRC
  'graphcool-1.0'?: DatabaseRC
}

export interface DatabaseRC {
  cloudSessionKey?: string
  clusters?: Clusters
}

export interface FrameworkRC {
  platformToken?: string
  clusters?: Clusters
}

export interface Clusters {
  [name: string]: ClusterConfig
}

export interface ClusterConfig {
  host: string
  clusterSecret: string
}
