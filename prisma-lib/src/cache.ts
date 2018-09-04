import { GraphQLSchema } from 'graphql'
import { importSchema } from 'graphql-import'
import { SharedLink } from './SharedLink'
import { makeRemoteExecutableSchema } from 'graphql-tools'

const typeDefsCache: { [schemaPath: string]: string } = {}
const remoteSchemaCache: { [typeDefs: string]: GraphQLSchema } = {}

export function getCachedTypeDefs(schemaPath: string): string {
  if (typeDefsCache[schemaPath]) {
    return typeDefsCache[schemaPath]
  }

  const schema = importSchema(schemaPath)
  typeDefsCache[schemaPath] = schema

  return schema
}

export function getCachedRemoteSchema(
  typeDefs: string,
  link: SharedLink,
): GraphQLSchema {
  if (remoteSchemaCache[typeDefs]) {
    return remoteSchemaCache[typeDefs]
  }

  const remoteSchema = makeRemoteExecutableSchema({
    // TODO fix typings
    link: link as any,
    schema: typeDefs,
  })
  remoteSchemaCache[typeDefs] = remoteSchema

  return remoteSchema
}
