export interface GraphcoolDefinition {
  types: string
  permissions: Permission[]
  functions: FunctionDefinition[]
  rootTokens: string[]
  modules: {[name: string]: string}
}

export interface Permission {
  description: string
  isEnabled?: boolean
  operation: string
  authenticated: boolean
  query?: string
  fields: string[]
}

export interface FunctionDefinition {
  name: string
  isEnabled: boolean
  handler: FunctionHandler
  type: FunctionType
  operation?: string
  query?: string
  schema?: string
}

export interface FunctionHandler {
  webhook?: FunctionHandlerWebhookSource
  code?: {
    src: string
  }
}

export interface FunctionHandlerWebhook {
  webhook: FunctionHandlerWebhookSource
}

export type FunctionHandlerWebhookSource = string | FunctionHandlerWebhookWithHeaders

export interface FunctionHandlerWebhookWithHeaders {
  url: string
  headers: Header[]
}

export interface Header {
  name: string
  value: string
}

export type FunctionType = 'operationBefore' | 'operationAfter' | 'subscription' | 'httpRequest' | 'httpResponse'
