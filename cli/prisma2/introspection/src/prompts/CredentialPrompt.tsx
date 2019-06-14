import chalk from 'chalk'
import { Box, Color, render } from 'ink'
import { DatabaseType } from 'prisma-datamodel'
import * as React from 'react'
import { credentialsToUri, uriToCredentials } from '../convertCredentials'
import {
  ConnectorData,
  getConnectedConnectorFromCredentials,
  getDatabaseSchemasWithMetadata,
  minimalPrettyTime,
} from '../introspect/util'
import { onFormChangedParams, OnSubmitParams, Prompt } from '../prompt-lib/BoxPrompt'
import {
  DatabaseCredentials,
  InitConfiguration,
  InitPromptResult,
  IntrospectionResult,
  PromptType,
  SchemaWithMetadata,
} from '../types'
import { ActionType, promptReducer } from './reducer'
import { Steps, stepsToElements } from './steps'
import figures = require('figures')

interface Props {
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
  const [state, dispatch] = React.useReducer<React.Reducer<PromptState, ActionType>>(promptReducer, initialState)

  switch (state.step) {
    case Steps.SELECT_DATABASE_TYPE:
      return (
        <Prompt
          key={Steps.SELECT_DATABASE_TYPE}
          title="What kind of database do you want to introspect?"
          elements={stepsToElements[Steps.SELECT_DATABASE_TYPE]()}
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
    case Steps.INPUT_DATABASE_CREDENTIALS:
      return (
        <Prompt
          key={Steps.INPUT_DATABASE_CREDENTIALS}
          elements={stepsToElements[Steps.INPUT_DATABASE_CREDENTIALS](state.credentials.type!)}
          title={`Enter ${dbTypeTodbName[state.credentials.type!]} credentials`}
          subtitle={`Learn how to set up a ${dbTypeTodbName[state.credentials.type!]} database: prisma.io/docs`}
          formValues={state.credentials}
          onFormChanged={onDatabaseCredentialsChanged(dispatch)}
          onSubmit={onConnectOrTest(state, dispatch)}
          withBackButton={{
            label: 'Back',
            description: '(Database selection)',
          }}
        />
      )
    case Steps.SELECT_DATABASE_SCHEMA:
      return (
        <Box flexDirection="column">
          <Color green>{figures.tick} Connected to database</Color>
          <Prompt
            key={Steps.SELECT_DATABASE_SCHEMA}
            title="Select the schema you want to introspect"
            formValues={state.credentials}
            elements={stepsToElements[Steps.SELECT_DATABASE_SCHEMA](state.schemas)}
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
            onSubmit={onSelectSchema(dispatch, state, props)}
            withBackButton={{
              label: 'Back',
              description: '(Database credentials)',
            }}
          />
        </Box>
      )
    case Steps.SELECT_TOOL:
      return (
        <Prompt
          key={Steps.SELECT_TOOL}
          title="Which parts of Prisma do you want to use?"
          subtitle="Learn more about the tools at prisma.io/docs"
          elements={stepsToElements[Steps.SELECT_TOOL]()}
          formValues={state}
          withBackButton={{
            label: 'Back',
            description: '(Select schema)',
          }}
          onFormChanged={params => {
            dispatch({
              type: 'set_tools',
              payload: {
                lift: params.values.lift === undefined ? false : params.values.lift,
                photon: params.values.photon === undefined ? false : params.values.photon,
              },
            })
          }}
          onSubmit={params => {
            if (params.goBack) {
              return dispatch({ type: 'back' })
            }

            if (params.selectedValue === '__CREATE__' && (state.lift || state.photon)) {
              dispatch({ type: 'set_step', payload: { step: Steps.SELECT_LANGUAGE } })
            } else {
              params.stopSpinner({ state: 'failed', message: 'Please, select at list one tool' })
            }
          }}
        />
      )
    case Steps.SELECT_LANGUAGE:
      return (
        <Prompt
          key={Steps.SELECT_LANGUAGE}
          title="Which parts of Prisma do you want to use?"
          subtitle="Learn more about the tools at prisma.io/docs"
          elements={stepsToElements[Steps.SELECT_LANGUAGE]()}
          formValues={{}}
          withBackButton={{
            label: 'Back',
            description: '(Tool selection)',
          }}
          onSubmit={params => {
            if (params.goBack) {
              return dispatch({ type: 'back' })
            }
            dispatch({ type: 'set_language', payload: { language: params.selectedValue } })
          }}
        />
      )
    case Steps.SELECT_TEMPLATE:
      return (
        <Prompt
          key={Steps.SELECT_TEMPLATE}
          title="Which parts of Prisma do you want to use?"
          subtitle="Learn more about the tools at prisma.io/docs"
          elements={stepsToElements[Steps.SELECT_TEMPLATE]()}
          formValues={{}}
          withBackButton={{
            label: 'Back',
            description: '(Language selection)',
          }}
          onSubmit={params => {
            if (params.goBack) {
              return dispatch({ type: 'back' })
            }

            const selectedTemplate = params.selectedValue

            state.connectorData.disconnect!()

            props.onSubmit({
              introspectionResult: state.introspectionResult,
              initConfiguration: {
                language: state.language,
                lift: state.lift,
                photon: state.photon,
                template: selectedTemplate,
              },
            } as InitPromptResult)
          }}
        />
      )
  }
}

function onDatabaseCredentialsChanged(
  dispatch: React.Dispatch<ActionType>,
): ((params: onFormChangedParams) => void) | undefined {
  return ({ values, triggeredInput }) => {
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
  }
}

function onSelectSchema(
  dispatch: React.Dispatch<ActionType>,
  state: PromptState,
  props: React.PropsWithChildren<Props>,
): (params: OnSubmitParams) => void {
  return async ({ goBack, startSpinner, stopSpinner }) => {
    if (goBack) {
      return dispatch({ type: 'back' })
    }

    const selectedSchema = state.credentials.newSchema ? state.credentials.newSchema : state.credentials.schema

    if (!state.credentials.schema && !state.credentials.newSchema) {
      stopSpinner({ state: 'failed', message: 'Please select a schema' })
      return
    }
    try {
      const before = Date.now()
      startSpinner(`Introspecting ${selectedSchema!}`)
      const credsWithNewSchemaReplaced = replaceSchemaByNewSchema(state.credentials as DatabaseCredentials)
      const credsWithDefaultCredentials = {
        ...defaultCredentials(state.credentials.type!),
        ...credsWithNewSchemaReplaced,
      }
      const introspectionResult = await props.introspect({
        ...state.connectorData,
        databaseName: selectedSchema,
        credentials: credsWithDefaultCredentials,
      } as ConnectorData)

      stopSpinner({
        state: 'succeeded',
        message: `Introspecting ${selectedSchema!} ${chalk.bold(minimalPrettyTime(Date.now() - before))}`,
      })

      if (props.type === 'introspect') {
        await state.connectorData.disconnect!()
        return props.onSubmit(introspectionResult)
      }

      if (props.type === 'init') {
        dispatch({ type: 'set_introspection_result', payload: { introspectionResult } })
      }
    } catch (e) {
      stopSpinner({ state: 'failed', message: e.message })
    }
  }
}

function onConnectOrTest(state: PromptState, dispatch: React.Dispatch<ActionType>): (params: OnSubmitParams) => void {
  return async params => {
    const newCredentials = {
      ...state.credentials,
      ...params.formValues,
    } as DatabaseCredentials

    if (params.goBack) {
      return dispatch({
        type: 'back',
        payload: {
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
    const schemas = await getDatabaseSchemasWithMetadata(connectorAndDisconnect.connector)

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
    params.stopSpinner({ state: 'failed', message: e.message })
  }
}

export async function promptInteractively(
  introspectFn: (connector: ConnectorData) => Promise<IntrospectionResult>,
  type: 'init',
): Promise<InitPromptResult>
export async function promptInteractively(
  introspectFn: (connector: ConnectorData) => Promise<IntrospectionResult>,
  type: 'introspect',
): Promise<IntrospectionResult>
export async function promptInteractively(
  introspectFn: (connector: ConnectorData) => Promise<IntrospectionResult>,
  type: PromptType,
): Promise<IntrospectionResult | InitPromptResult> {
  return new Promise(async resolve => {
    render(<IntrospectionPrompt introspect={introspectFn} type={type} onSubmit={resolve} />)
  })
}
