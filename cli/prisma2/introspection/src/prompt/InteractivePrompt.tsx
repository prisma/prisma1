import { render } from 'ink'
import { DatabaseType } from 'prisma-datamodel'
import * as React from 'react'
import { ConnectorData } from '../introspect/util'
import {
  DatabaseCredentials,
  InitConfiguration,
  InitPromptResult,
  IntrospectionResult,
  PromptType,
  SchemaWithMetadata,
} from '../types'
import { ActionType, promptReducer } from './reducer'
import {
  renderInputDatabaseCredentials,
  renderSelectDatabaseSchema,
  renderSelectDatabaseType,
  renderSelectLanguage,
  renderSelectTemplate,
  renderSelectTool,
} from './steps'
import { Steps } from './steps-definition'

export interface PromptProps {
  onSubmit: (introspectionResult: IntrospectionResult | InitPromptResult) => void
  introspect: (connector: ConnectorData) => Promise<IntrospectionResult>
  type: PromptType
}

export type PromptState = {
  step: Steps
  credentials: Partial<DatabaseCredentials>
  connectorData: Partial<ConnectorData>
  introspectionResult: IntrospectionResult | null
  schemas: SchemaWithMetadata[]
} & InitConfiguration

const initialState: PromptState & InitConfiguration = {
  step: Steps.SELECT_DATABASE_TYPE,
  credentials: {},
  connectorData: {},
  schemas: [],
  introspectionResult: null,
  lift: false,
  photon: false,
  language: 'TypeScript',
  template: 'from_scratch',
  databaseType: DatabaseType.sqlite
}

export const dbTypeTodbName: Record<DatabaseType, string> = {
  [DatabaseType.postgres]: 'Postgres',
  [DatabaseType.mysql]: 'MySQL',
  [DatabaseType.mongo]: 'MongoDB',
  [DatabaseType.sqlite]: 'SQLite',
}

export const defaultCredentials = (dbType: DatabaseType): DatabaseCredentials => ({
  host: 'localhost',
  port: 5432,
  type: dbType,
})

/**
 * WARNING: If you add more steps, make sure to add a `key` to the `<Prompt />`, otherwise the state between each prompt will be shared
 */
export const InteractivePrompt: React.FC<PromptProps> = props => {
  const [state, dispatch] = React.useReducer<React.Reducer<PromptState, ActionType>>(promptReducer, initialState)

  switch (state.step) {
    case Steps.SELECT_DATABASE_TYPE:
      return renderSelectDatabaseType(dispatch, state)
    case Steps.INPUT_DATABASE_CREDENTIALS:
      return renderInputDatabaseCredentials(dispatch, state)
    case Steps.SELECT_DATABASE_SCHEMA:
      return renderSelectDatabaseSchema(dispatch, state, props)
    case Steps.SELECT_TOOL:
      return renderSelectTool(dispatch, state)
    case Steps.SELECT_LANGUAGE:
      return renderSelectLanguage(dispatch)
    case Steps.SELECT_TEMPLATE:
      return renderSelectTemplate(dispatch, state, props)
  }
}
