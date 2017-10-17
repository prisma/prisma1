export interface RC {
  platformToken?: string
  clusters?: Clusters
  targets: Targets
}

export interface Targets {
  [name: string]: Target
}

export interface Target {
  cluster: string
  id: string
}

export interface Clusters {
  default?: string
  [name: string]: Cluster | string | undefined
}

export interface Cluster {
  host: string
  faasHost: string
  clusterSecret: string
}

export interface InternalRC {
  platformToken?: string
  clusters?: Clusters
  targets?: InternalTargets
}

export interface InternalTargets {
  [name: string]: string
}
