import { Prisma } from './generated/prisma'
import { GraphQLServer } from 'graphql-yoga'
import { S3 } from 'aws-sdk'
import { resolvers } from './resolvers'
import fileApi from './modules/fileApi'

const s3client = new S3({
  accessKeyId: process.env.S3_ACCESS_KEY_ID,
  secretAccessKey: process.env.S3_SECRET_ACCESS_KEY,
  params: {
    Bucket: process.env.S3_BUCKET,
  },
})

const prisma = new Prisma({
  endpoint: process.env.PRISMA_ENDPOINT,
  secret: process.env.PRISMA_SECRET,
  debug: true,
})

const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: prisma,
  }),
})

server.express.post(
  '/upload',
  fileApi({
    s3: s3client,
    prisma,
  }),
)

server.start(() => console.log(`Server is running on http://localhost:5000`))
