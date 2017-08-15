export interface GCL {
  databaseSchema: DatabaseSchema
  modelPermissions: ModelPermission[]
  relationPermissions: RelationPermission[]
  functions: FunctionDefinition[]
  pats: PermanentAuthToken[]
}

export interface DatabaseSchema {
  src: string
}

export interface ModelPermission {
  description: string
  isEnabled: boolean
  operation: string
  authenticated: boolean
  query?: Query
  fields: string[]
}

export interface Query {
  src: string
}

export interface RelationPermission {
  isEnabled: boolean
  relation: string
  connect: boolean
  disconnect: boolean
  authenticated: boolean
  query?: Query
}

export interface FunctionDefinition {
  name: string
  isEnabled: boolean
  handler: FunctionHandler
  serversideSubscription?: ServersideSubscription
}

interface ServersideSubscription {
  subscriptionQuery?: Query
  schemaExtension?: Query
}

export interface FunctionHandler {
  code?: FunctionHandlerCode
  webhook?: FunctionHandlerWebhook
}

export interface FunctionHandlerCode {
  src: string
}

export interface FunctionHandlerWebhook {
  url: string
  headers: Header[]
}

export interface Header {
  name: string
  value: string
}

export interface PermanentAuthToken {
  name: string
  description: string
}

export interface FunctionEvent {
  serverSideSubscription: FunctionEventServerSideSubscription
  middleware: FunctionEventMiddleware
  schemaExtension: FunctionEventSchemaExtension
  nodeCallback: FunctionEventNodeCallback
}

export interface FunctionEventServerSideSubscription {
  subscriptionQuery: Query
}

export interface FunctionEventMiddleware {
  order: number
}

export interface FunctionEventSchemaExtension {
  schema: string
}

export interface FunctionEventNodeCallback {
  target: string
  order: number
  operation: FunctionEventNodeCallbackOperation
  step: FunctionEventNodeCallbackStep
}

export type FunctionEventNodeCallbackOperation  =
    'Create'
  | 'Update'
  | 'Delete'
  | 'Connect'
  | 'Disconnect'

export type FunctionEventNodeCallbackStep =
    'BeforeValidation'
  | 'AfterValidation'
  | 'AfterWrite'
