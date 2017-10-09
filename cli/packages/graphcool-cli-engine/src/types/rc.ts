export interface RC {
  platformToken?: string
  clusters?: Clusters
  targets?: Targets
}

export interface Targets {
  [name: string]: Target
}

export interface Target {
  cluster: string
  id: string
}

export interface Clusters {[name: string]: Cluster}

export interface Cluster {
  host: string
  token: string
}
