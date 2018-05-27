import { ClientConfig } from 'pg'

export type TypeIdentifier =
  | 'String'
  | 'Int'
  | 'Float'
  | 'Boolean'
  | 'DateTime'
  | 'ID'
  | 'Json' // | 'Enum' | 'Relation'

export interface Relation {
  table: string
}

export interface Column {
  name: string
  isPrimaryKey: boolean
  isUnique: boolean
  defaultValue: any
  type: string
  typeIdentifier: TypeIdentifier
  comment: string | null
  relation: Relation | null
  nullable: Boolean
}

export interface Table {
  name: string
  isJunctionTable: boolean
  columns: Column[]
}

export interface PrimaryKey {
  tableName: string
  fields: string[]
}

export type PostgresConnectionDetails = string | ClientConfig
