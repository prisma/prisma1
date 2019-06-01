import { DatabaseType } from 'prisma-datamodel'

export interface DatabaseCredentials {
  type: DatabaseType
  host?: string
  port?: number
  user?: string
  password?: string
  database?: string
  alreadyData?: boolean
  schema?: string
  ssl?: boolean
  uri?: string
  executeRaw?: boolean
}
