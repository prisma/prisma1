const { GraphQLServer } = require('graphql-yoga')
const { importSchema } = require('graphql-import')
const { resolvers } = require('./dog-api/resolvers')

const typeDefs = importSchema('./src/schema.graphql')

const server = new GraphQLServer({
  typeDefs,
  resolvers,
 })

server.start(() => console.log('Server is running on http://localhost:4000'))
