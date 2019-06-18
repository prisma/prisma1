import { DatabaseType } from 'prisma-datamodel'
import { DatabaseMetadata } from 'prisma-db-introspection/dist/common/introspectionResult'
import { ConnectorAndDisconnect } from '../introspect/util'
import { DatabaseCredentials, IntrospectionResult } from '../types'
import { PromptState } from './InteractivePrompt'
import { Steps, stepToPrevStep } from './steps-definition'

export type ActionChooseDB = {
  type: 'choose_db'
  payload: DatabaseType
}

export type ActionConnect = {
  type: 'connect_db'
  payload: {
    credentials: DatabaseCredentials
    schemas: { name: string; metadata: DatabaseMetadata }[]
    connectorAndDisconnect: ConnectorAndDisconnect
  }
}

export type ActionBack = {
  type: 'back'
  payload?: {
    prevStep?: Steps
    credentials?: DatabaseCredentials
  }
}

export type ActionSetCredentials = {
  type: 'set_credentials'
  payload: {
    credentials: DatabaseCredentials
  }
}

export type ActionSetTools = {
  type: 'set_tools'
  payload: {
    lift: boolean
    photon: boolean
  }
}

export type ActionSetLanguage = {
  type: 'set_language'
  payload: {
    language: PromptState['language']
  }
}

export type ActionSetTemplate = {
  type: 'set_template'
  payload: {
    template: PromptState['template']
  }
}

export type ActionSetStep = {
  type: 'set_step'
  payload: {
    step: Steps
  }
}

export type ActionSetIntrospectionResult = {
  type: 'set_introspection_result'
  payload: {
    introspectionResult: IntrospectionResult
  }
}

export type ActionType =
  | ActionChooseDB
  | ActionConnect
  | ActionBack
  | ActionSetCredentials
  | ActionSetTools
  | ActionSetStep
  | ActionSetLanguage
  | ActionSetTemplate
  | ActionSetIntrospectionResult

export const promptReducer: React.Reducer<PromptState, ActionType> = (state, action) => {
  switch (action.type) {
    case 'choose_db':
      return {
        ...state,
        step: action.payload === 'sqlite' ? Steps.SELECT_TOOL : Steps.INPUT_DATABASE_CREDENTIALS,
        credentials: {
          ...state.credentials,
          type: action.payload,
        },
      }
    case 'connect_db':
      return {
        ...state,
        step: Steps.SELECT_DATABASE_SCHEMA,
        credentials: {
          ...state.credentials,
          ...action.payload.credentials,
        },
        schemas: action.payload.schemas,
        connectorData: action.payload.connectorAndDisconnect,
      }
    case 'back':
      if (!action.payload) {
        action.payload = {}
      }

      if (!action.payload.prevStep) {
        action.payload.prevStep = stepToPrevStep[state.step]
      }

      return {
        ...state,
        step: action.payload.prevStep,
        credentials: !action.payload.credentials
          ? state.credentials
          : { ...state.credentials, ...action.payload.credentials },
      }
    case 'set_step':
      return {
        ...state,
        step: action.payload.step,
      }
    case 'set_credentials':
      return {
        ...state,
        credentials: {
          ...state.credentials,
          ...action.payload.credentials,
        },
      }
    case 'set_introspection_result':
      return {
        ...state,
        introspectionResult: action.payload.introspectionResult,
        step: Steps.SELECT_TOOL,
      }
    case 'set_tools':
      return {
        ...state,
        lift: action.payload.lift,
        photon: action.payload.photon,
      }
    case 'set_language':
      return {
        ...state,
        ...action.payload,
        step: Steps.SELECT_TEMPLATE,
      }
    case 'set_template':
      return {
        ...state,
        template: action.payload.template,
      }
  }
}
