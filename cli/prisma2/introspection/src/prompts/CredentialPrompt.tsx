import chalk from 'chalk'
import { render } from 'ink'
import { DatabaseType } from 'prisma-datamodel'
import * as React from 'react'
import { credentialsToUri, uriToCredentials } from '../convertCredentials'
import {
  ConnectorAndDisconnect,
  ConnectorData,
  getConnectedConnectorFromCredentials,
  getDatabaseSchemas,
  minimalPrettyTime,
} from '../introspect/util'
import { OnSubmitParams, Prompt } from '../prompt-lib/BoxPrompt'
import { RadioElement } from '../prompt-lib/types'
import { DatabaseCredentials, IntrospectionResult } from '../types'
import { CHOOSE_DB_ELEMENTS, CONNECT_DB_ELEMENTS } from './prompts-elements'

enum Steps {
  CHOOSE_DB,
  CONNECT_DB,
  CHOOSE_SCHEMA,
}

interface Props {
  onSubmit: (introspectionResult: IntrospectionResult) => void
  introspect: (connector: ConnectorData) => Promise<IntrospectionResult>
}

interface State {
  step: Steps
  credentials: Partial<DatabaseCredentials>
  connectorData: Partial<ConnectorData>
  schemas: string[]
}

type ActionChooseDB = {
  type: 'choose_db'
  payload: DatabaseType
}

type ActionConnect = {
  type: 'connect_db'
  payload: {
    credentials: DatabaseCredentials
    schemas: string[]
    connectorAndDisconnect: ConnectorAndDisconnect
  }
}

type ActionBack = {
  type: 'back'
  payload: {
    prevStep: Steps
    credentials?: DatabaseCredentials
  }
}

type ActionSetCredentials = {
  type: 'set_credentials'
  payload: {
    credentials: DatabaseCredentials
  }
}

type ActionType = ActionChooseDB | ActionConnect | ActionBack | ActionSetCredentials

const initialState: State = {
  step: Steps.CHOOSE_DB,
  credentials: {},
  connectorData: {},
  schemas: [],
}

const reducer: React.Reducer<State, ActionType> = (state, action) => {
  switch (action.type) {
    case 'choose_db':
      return {
        ...state,
        step: Steps.CONNECT_DB,
        credentials: {
          ...state.credentials,
          type: action.payload,
        },
      }
    case 'connect_db':
      return {
        ...state,
        step: Steps.CHOOSE_SCHEMA,
        credentials: {
          ...state.credentials,
          ...action.payload.credentials,
        },
        schemas: action.payload.schemas,
        connectorData: action.payload.connectorAndDisconnect,
      }
    case 'back':
      return {
        ...state,
        step: action.payload.prevStep,
        credentials: !action.payload.credentials
          ? state.credentials
          : { ...state.credentials, ...action.payload.credentials },
      }
    case 'set_credentials':
      return {
        ...state,
        credentials: {
          ...state.credentials,
          ...action.payload.credentials,
        },
      }
  }
}

const dbTypeTodbName: Record<DatabaseType, string> = {
  [DatabaseType.postgres]: 'Postgres',
  [DatabaseType.mysql]: 'MySQL',
  [DatabaseType.mongo]: 'MongoDB',
  [DatabaseType.sqlite]: 'SQLite',
}

const defaultCredentials = (dbType: DatabaseType): DatabaseCredentials => ({
  host: 'localhost',
  port: 5432,
  type: dbType,
})

function replaceSchemaByNewSchema(credentials: DatabaseCredentials): DatabaseCredentials {
  if (credentials.newSchema) {
    return {
      ...credentials,
      schema: credentials.newSchema,
    }
  }

  return credentials
}

/**
 * WARNING: If you add more steps, make sure to add a `key` to the `<Prompt />`, otherwise the state between each prompt will be shared
 */
const IntrospectionPrompt: React.FC<Props> = props => {
  const [state, dispatch] = React.useReducer<React.Reducer<State, ActionType>>(reducer, initialState)

  switch (state.step) {
    case Steps.CHOOSE_DB:
      return (
        <Prompt
          key={Steps.CHOOSE_DB}
          title="What kind of database do you want to introspect?"
          elements={CHOOSE_DB_ELEMENTS}
          onSubmit={({ selectedValue }) => {
            dispatch({
              type: 'choose_db',
              payload: selectedValue,
            })
          }}
          formValues={state.credentials}
          withBackButton={false}
        />
      )
    case Steps.CONNECT_DB:
      return (
        <Prompt
          key={Steps.CONNECT_DB}
          elements={CONNECT_DB_ELEMENTS(state.credentials.type!)}
          title={`Enter ${dbTypeTodbName[state.credentials.type!]} credentials`}
          subtitle={`Learn how to set up a ${dbTypeTodbName[state.credentials.type!]} database: prisma.io/docs`}
          formValues={state.credentials}
          onFormChanged={({ values, triggeredInput }) => {
            let credentials: DatabaseCredentials = {
              ...(values as DatabaseCredentials),
            }

            if (triggeredInput.identifier === 'uri') {
              try {
                credentials = uriToCredentials(values['uri'])
              } catch {}
            } else {
              credentials['uri'] = credentialsToUri({
                ...defaultCredentials(credentials['type']),
                ...credentials,
              } as DatabaseCredentials)
            }

            dispatch({ type: 'set_credentials', payload: { credentials } })
          }}
          onSubmit={onConnectOrTest(state, dispatch)}
          withBackButton={{
            label: 'Back',
            description: '(Database selection)',
          }}
        />
      )
    case Steps.CHOOSE_SCHEMA:
      return (
        <Prompt
          key={Steps.CHOOSE_SCHEMA}
          title="Select the schema you want to introspect"
          formValues={state.credentials}
          elements={[
            ...state.schemas.map(
              schema =>
                ({
                  type: 'radio',
                  label: schema,
                  value: schema,
                  identifier: 'schema',
                  description: '5 tables, 1,114KB',
                } as RadioElement),
            ),
            {
              type: 'text-input',
              identifier: 'newSchema',
              label: 'New schema',
              placeholder: 'Or enter a name for a new schema',
              style: { marginTop: 1 },
            },
            { type: 'separator', style: { marginTop: 1, marginBottom: 1, marginLeft: 1 } },
            { type: 'select', label: 'Introspect', description: '(Please select a schema)', style: { marginTop: 1 } },
          ]}
          onFormChanged={({ values, triggeredInput }) => {
            // Select only one or the other
            if (triggeredInput.identifier === 'schema') {
              values['newSchema'] = ''
            }
            if (triggeredInput.identifier === 'newSchema') {
              values['schema'] = ''
            }

            dispatch({ type: 'set_credentials', payload: { credentials: values as DatabaseCredentials } })
          }}
          onSubmit={async ({ selectedValue, goBack, startSpinner, stopSpinner }) => {
            if (goBack) {
              return dispatch({
                type: 'back',
                payload: { prevStep: Steps.CONNECT_DB },
              })
            }

            if (!state.credentials.schema && !state.credentials.newSchema) {
              stopSpinner({ state: 'failed', message: 'Please select a schema' })
              return
            }

            try {
              const before = Date.now()
              startSpinner(`Introspecting ${selectedValue!}`)

              const introspectionResult = await props.introspect({
                ...state.connectorData,
                databaseName: selectedValue,
                credentials: replaceSchemaByNewSchema(state.credentials as DatabaseCredentials),
              } as ConnectorData)

              await state.connectorData.disconnect!()

              stopSpinner({
                state: 'succeeded',
                message: `Introspecting ${selectedValue!} ${chalk.bold(minimalPrettyTime(Date.now() - before))}`,
              })

              props.onSubmit(introspectionResult)
            } catch (e) {
              stopSpinner({ state: 'failed', message: e.message })
            }
          }}
          withBackButton={{
            label: 'Back',
            description: '(Database credentials)',
          }}
        />
      )
  }
}

function onConnectOrTest(state: State, dispatch: React.Dispatch<ActionType>): (params: OnSubmitParams) => void {
  return async params => {
    const newCredentials = {
      ...state.credentials,
      ...params.formValues,
    } as DatabaseCredentials

    if (params.goBack) {
      return dispatch({
        type: 'back',
        payload: {
          prevStep: Steps.CHOOSE_DB,
          credentials: newCredentials,
        },
      })
    }

    if (params.selectedValue === '__CONNECT__') {
      await onConnect(params, newCredentials, dispatch)
    }
  }
}

async function onConnect(
  params: OnSubmitParams,
  credentials: DatabaseCredentials,
  dispatch: React.Dispatch<ActionType>,
) {
  params.startSpinner()

  try {
    const connectorAndDisconnect = await getConnectedConnectorFromCredentials(credentials)
    const schemas = await getDatabaseSchemas(connectorAndDisconnect.connector)

    params.stopSpinner({ state: 'succeeded' })

    if (schemas) {
      dispatch({
        type: 'connect_db',
        payload: {
          schemas,
          connectorAndDisconnect,
          credentials,
        },
      })
    }
  } catch (e) {
    params.stopSpinner({ state: 'failed', message: e.message }) // TODO: Display error message on the prompt
  }
}

export async function promptIntrospectionInteractively(
  introspectFn: (connector: ConnectorData) => Promise<IntrospectionResult>,
) {
  return new Promise<IntrospectionResult>(async resolve => {
    render(<IntrospectionPrompt introspect={introspectFn} onSubmit={resolve} />)
  })
}
