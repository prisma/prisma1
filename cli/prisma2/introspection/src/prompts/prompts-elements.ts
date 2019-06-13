import { DatabaseType } from 'prisma-datamodel'
import { PromptElement } from '../prompt-lib/types'
import { DatabaseCredentials } from '../types'

const dbTypeToDbPort: Record<DatabaseType, string> = {
  postgres: '5432',
  mysql: '3306',
  sqlite: '3306',
  mongo: '3306',
}

const dbTypeToDefaultConnectionString: Record<DatabaseType, string> = {
  postgres: `postgresql://localhost:${dbTypeToDbPort[DatabaseType.postgres]}`,
  mysql: `mysql://localhost:${dbTypeToDbPort[DatabaseType.mysql]}`,
  sqlite: `sqlite://localhost:${dbTypeToDbPort[DatabaseType.sqlite]}`,
  mongo: `mongo://localhost:${dbTypeToDbPort[DatabaseType.mongo]}`,
}

export const CONNECT_DB_ELEMENTS = (dbType: DatabaseType): PromptElement<DatabaseCredentials>[] => [
  {
    type: 'text-input',
    identifier: 'host',
    label: 'Host:',
    placeholder: 'localhost',
  },
  {
    type: 'text-input',
    identifier: 'port',
    label: 'Port:',
    placeholder: dbTypeToDbPort[dbType],
  },
  {
    type: 'text-input',
    identifier: 'user',
    label: 'User:',
  },
  {
    type: 'text-input',
    identifier: 'password',
    label: 'Password:',
  },
  {
    type: 'text-input',
    identifier: 'database',
    label: 'Database:',
    style: { marginBottom: 1 },
  },
  {
    type: 'checkbox',
    label: 'Enable SSL ?',
    identifier: 'ssl',
    style: { marginBottom: 1 },
  },
  { type: 'separator', style: { marginBottom: 1 } },
  {
    type: 'text-input',
    identifier: 'uri',
    label: 'Or URL',
    placeholder: dbTypeToDefaultConnectionString[dbType],
    style: { marginBottom: 1 },
  },
  { type: 'separator', style: { marginBottom: 1 } },
  {
    type: 'select',
    label: 'Connect',
    value: '__CONNECT__',
  },
]

export const CHOOSE_DB_ELEMENTS: PromptElement[] = [
  {
    type: 'select',
    label: 'MySQL',
    value: 'mysql',
    description: 'MySQL compliant databases like MySQL or MariaDB',
  },
  {
    type: 'select',
    label: 'Postgres',
    value: 'postgres',
    description: 'PostgreSQL database',
  },
  // {
  //   type: 'select',
  //   label: 'MongoDB',
  //   value: 'mongodb',
  //   description: 'Mongo Database',
  // },
]
