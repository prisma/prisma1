import { Prisma } from './generated/prisma'
import { GraphQLServer } from 'graphql-yoga'
import { S3 } from 'aws-sdk'
import { resolvers } from './resolvers'
import fileApi from './modules/fileAPI'

const s3client = new S3({
  accessKeyId: process.env.S3_ACCESS_KEY_ID,
  secretAccessKey: process.env.S3_SECRET_ACCESS_KEY,
  params: {
    Bucket: process.env.S3_BUCKET,
  },
})

const getPrismaInstance = () => {
  return new Prisma({
    typeDefs: 'src/generated/prisma.graphql',
    endpoint: process.env.PRISMA_ENDPOINT,  // Prisma service endpoint (see `~/.prisma/config.yml`)
    secret: process.env.PRISMA_SECRET,      // `secret` taken from `prisma.yml`
    debug: true                             // log all requests to the Prisma API to console
  })
}

const server = new GraphQLServer({
  typeDefs: './src/schema.graphql',
  resolvers,
  context: req => ({
    ...req,
    db: getPrismaInstance(),
  })
})

server.express.post(
  '/upload',
  fileApi({
    s3: s3client,
    prisma: getPrismaInstance()
  })
)

server.start(() => console.log(`Server is running on http://localhost:4000`))
