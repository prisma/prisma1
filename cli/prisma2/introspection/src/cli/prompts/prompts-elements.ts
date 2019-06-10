import { PromptElement } from '../prompt-lib/types'
import { DatabaseCredentials } from '../types'
import { DatabaseType } from 'prisma-datamodel'

export const CONNECT_DB_ELEMENTS = (
  dbType: DatabaseType,
): PromptElement<DatabaseCredentials>[] => [
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
    placeholder: dbType === DatabaseType.postgres ? '5432' : '3306',
  },
  {
    type: 'text-input',
    identifier: 'user',
    label: 'User:',
    placeholder: 'my_db_user',
  },
  {
    type: 'text-input',
    identifier: 'password',
    label: 'Password:',
    placeholder: 'my_db_password',
    mask: '*',
  },
  {
    type: 'text-input',
    identifier: 'database',
    label: 'Database:',
    placeholder: 'my_db_name',
    style: { marginBottom: 1 },
  },
  {
    type: 'checkbox',
    label: 'Enable SSL ?',
    identifier: 'ssl',
    style: { marginBottom: 1 },
  },
  { type: 'separator', dividerChar: '-', style: { marginBottom: 1 } },
  {
    type: 'select',
    label: 'Test',
    value: '__TEST__',
    description: 'Test the database connection',
  },
  {
    type: 'select',
    label: 'Connection',
    value: '__CONNECT__',
    description: 'Start the introspection',
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
