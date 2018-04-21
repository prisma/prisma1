import { makeExecutableSchema, addMockFunctionsToSchema } from 'graphql-tools'
import { graphql } from 'graphql'
import * as fs from 'fs-extra'

const typeDefs = fs.readFileSync('./cluster.graphql', 'utf-8')

const schema = makeExecutableSchema({ typeDefs })

addMockFunctionsToSchema({
  schema,
  mocks: {
    Migration: () => ({
      revision: 5,
    }),
  },
})

export const MockGraphQLClient = () => {
  return {
    request(query, variables) {
      return graphql(schema, query, {}, {}, variables)
    },
  }
}
