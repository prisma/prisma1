export interface User {
  id: string
  name: string
  login: Login[]
}

export interface Login {
  email: string
}

export interface Migration {
  projectId: string
  revision: number
  status: string
  applied: number
  rolledBack: number
  errors: string[]
}

export interface DeployPayload {
  errors: SchemaError[]
  warnings: SchemaError[]
  migration: Migration
  steps?: MigrationStep[]
}

export interface AuthenticationPayload {
  isAuthenticated: boolean
  account: User | null
}

export interface SchemaError {
  type: string
  field?: string
  description: string
}

export interface Migration {
  revision: number
  hasBeenApplied: boolean
  steps: MigrationStep[]
}

export interface MigrationStep {
  type: string
  __typename?: string | null
  name: string
  // createEnum
  ce_values?: string[] | null
  // createField
  cf_typeName?: string | null
  cf_isRequired?: boolean | null
  cf_isList?: boolean | null
  cf_isUnique?: boolean | null
  cf_relation?: string | null
  cf_defaultValue?: string | null
  cf_enum?: string | null
  // createRelation
  leftModel?: string | null
  rightModel?: string | null
  // deleteField
  model?: string | null
  // updateEnum
  newName?: string | null
  values?: string[] | null
  // updateField
  typeName?: string | null
  isUnique?: boolean | null
  isRequried?: boolean | null
  isList?: boolean | null
  isRequired?: boolean | null
  relation?: string | null
  defaultValue?: string | null
  enum?: string | null
  // updateModel
  um_newName?: string | null
}

export interface Workspace {
  id: string
  name: string
  slug: string
  clusters: Cluster[]
}

export interface Cluster {
  id: string
  name: string
  connectInfo: {
    endpoint: string
  }
}

export interface Service {
  id: string
  stage: string
  name: string
  cluster: {
    name: string
  }
}
