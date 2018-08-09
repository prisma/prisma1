import * as express from 'express'
import * as cors from 'cors'
import * as bodyParser from 'body-parser'
import { graphqlExpress } from 'apollo-server-express'
import GraphQLServerOptions from 'apollo-server-core/dist/graphqlOptions'
import playground from 'graphql-playground-middleware-express'
import mergeSchemas from 'graphql-tools/dist/stitching/mergeSchemas'

import getGraphcoolSchema from './graphcool-schema'

// API extensions
import { resolver as HelloWorldResolver, typeDefs as HelloWorldTypeDefs } from './src/hello-world'

const app = express()

getGraphcoolSchema()
  .then((schema) => {
    app.use(cors())

    const mergedSchemas = mergeSchemas({
      schemas: [
        schema,
        HelloWorldTypeDefs
      ],
      // This can easily be extended to support multiple files/extensions utilizing basic schema stitching
      resolvers: mergeInfo => HelloWorldResolver(mergeInfo)
    })

    // Api routes
    app.use(
      '/graphql',
      cors(),
      bodyParser.json(),
      graphqlExpress({ schema: mergedSchemas } as GraphQLServerOptions)
    )

    if (process.env.NODE_ENV !== 'production') {
      app.use('/playground', playground({ endpoint: '/graphql' }))
    }

    app.listen(process.env.GATEWAY_PORT, () => {
      console.log(`Listening on port ${process.env.GATEWAY_PORT}`)
      console.log(`Access the playground: http://localhost:${process.env.GATEWAY_PORT}/playground`)
    })
  })
  .catch((e) => {
    console.error(e.message)
  })
