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

export type PostgresConnectionDetails = string | ClientConfig
