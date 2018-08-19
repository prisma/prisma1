import { print, OperationDefinitionNode } from 'graphql'
import { ApolloLink, Operation, split } from 'apollo-link'
import { WebSocketLink } from 'apollo-link-ws'
import { onError } from 'apollo-link-error'
import * as ws from 'ws'
import { HTTPLinkDataloader } from 'http-link-dataloader'

export function makePrismaLink({
  endpoint,
  token,
  debug,
}: {
  endpoint: string
  token?: string
  debug?: boolean
}): ApolloLink {
  const httpLink = new HTTPLinkDataloader({
    uri: endpoint,
    headers: token ? { Authorization: `Bearer ${token}` } : {},
  })

  // also works for https/wss
  const wsEndpoint = endpoint.replace(/^http/, 'ws')
  const wsLink = new WebSocketLink({
    uri: wsEndpoint,
    options: {
      reconnect: true,
      connectionParams: token
        ? {
            Authorization: `Bearer ${token}`,
          }
        : {},
      lazy: true,
      inactivityTimeout: 30000,
    },
    webSocketImpl: ws,
  })

  // TODO fix link typings
  const backendLink = split(op => isSubscription(op), wsLink, httpLink as any)

  const reportErrors = onError(({ graphQLErrors, networkError }) => {
    if (graphQLErrors)
      graphQLErrors.map(({ message, locations, path }) =>
        console.log(
          `[GraphQL error]: Message: ${message}, Location: ${locations}, Path: ${path}`,
        ),
      )
    if (networkError) console.log(`[Network error]: ${networkError}`)
  })

  if (debug) {
    const debugLink = new ApolloLink((operation, forward) => {
      console.log(`Request to ${endpoint}:`)
      console.log(`query:`)
      console.log(print(operation.query).trim())
      console.log(`operationName: ${operation.operationName}`)
      console.log(`variables:`)
      console.log(JSON.stringify(operation.variables, null, 2))

      return forward!(operation).map(data => {
        console.log(`Response from ${endpoint}:`)
        console.log(JSON.stringify(data.data, null, 2))
        return data
      })
    })

    return ApolloLink.from([debugLink, reportErrors, backendLink])
  } else {
    return ApolloLink.from([reportErrors, backendLink])
  }
}

function isSubscription(operation: Operation): boolean {
  const selectedOperation = getSelectedOperation(operation)
  if (selectedOperation) {
    return selectedOperation.operation === 'subscription'
  }
  return false
}

function getSelectedOperation(
  operation: Operation,
): OperationDefinitionNode | undefined {
  if (operation.query.definitions.length === 1) {
    return operation.query.definitions[0] as OperationDefinitionNode
  }

  return operation.query.definitions.find(
    d =>
      d.kind === 'OperationDefinition' &&
      !!d.name &&
      d.name.value === operation.operationName,
  ) as OperationDefinitionNode
}
