export interface DeployPayload {
  project: SimpleProject
  errors: SchemaError[]
  migration: Migration[]
}

export interface SchemaError {
  type: string
  field?: string
  description: string
}

export interface SimpleProject {
  id: string
}

export interface Migration {
  revision: number
  hasBeenApplied: boolean
  steps: MigrationStep[]
}

export interface MigrationStep {
  type: string
  name: string
  // createEnum
  ce_values?: string
  // createField
  cf_typeName?: string
  cf_isRequired?: boolean
  cf_isList?: boolean
  cf_isUnique?: boolean
  cf_relation?: string
  cf_defaultValue?: string
  cf_enum?: string
  // createRelation
  leftModel?: string
  rightModel?: string
  // deleteField
  model?: string
  // updateEnum
  newName?: string
  values?: string[]
  // updateField
  typeName?: string
  isRequried?: boolean
  isList?: boolean
  isRequired?: boolean
  relation?: string
  defaultValue?: string
  enum?: string
  // updateModel
  um_newName: string
}
