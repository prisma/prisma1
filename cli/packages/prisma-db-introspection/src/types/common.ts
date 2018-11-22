import { ClientConfig } from 'pg'

export type TypeIdentifier =
  | 'String'
  | 'Int'
  | 'Float'
  | 'Boolean'
  | 'DateTime'
  | 'ID'
  | 'UUID'
  | 'Json' // | 'Enum' | 'Relation'

export type PostgresConnectionDetails = string | ClientConfig