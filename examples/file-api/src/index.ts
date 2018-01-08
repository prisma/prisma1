import { Graphcool } from "./generated/graphcool"
import { importSchema } from "graphql-import"
import { GraphQLServer } from "graphql-yoga"
import { S3 } from 'aws-sdk'
import { resolvers } from "./resolvers"
import fileApi from './modules/fileApi'

// Config --------------------------------------------------------------------

const APP_SCHEMA_PATH = './src/schema.graphql'

const typeDefs = importSchema(APP_SCHEMA_PATH)

const s3client = new S3({
  accessKeyId: process.env.S3_KEY,
  secretAccessKey: process.env.S3_SECRET,
  params: {
    Bucket: process.env.S3_BUCKET
  }
})


// Server --------------------------------------------------------------------

const server = new GraphQLServer({
  typeDefs,
  resolvers,
  context: req => ({
    ...req,
    db: new Graphcool({
      endpoint: process.env.GRAPHCOOL_ENDPOINT,
      secret: process.env.GRAPHCOOL_SECRET,
    }),
  }),
})

// Middleware ----------------------------------------------------------------

server.express.post('/upload', fileApi({
  s3: s3client,
  graphcool: new Graphcool({
    endpoint: process.env.GRAPHCOOL_ENDPOINT,
    secret: process.env.GRAPHCOOL_SECRET,
  }),
}))

// Start ---------------------------------------------------------------------

server.start({ port: 5000 }, () => {
  console.log(`Server is running on http://localhost:5000`)
})

// ---------------------------------------------------------------------------
