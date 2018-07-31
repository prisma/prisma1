const { GraphQLServer } = require('graphql-yoga');
const { Prisma } = require('prisma-binding');
const Email = require('email-templates');
const { graphqlAuthenticationConfig } = require('graphql-authentication');
const {
  GraphqlAuthenticationPrismaAdapter,
} = require('graphql-authentication-prisma');
const { permissions } = require('./permissions');
const { resolvers } = require('./resolvers');

const APP_SECRET = 'appsecret321';

const mailer = new Email({
  message: {
    from: 'me@example.com',
  },
});

const server = new GraphQLServer({
  typeDefs: 'src/schema/schema.graphql',
  resolvers,
  resolverValidationOptions: {
    requireResolversForResolveType: false,
  },
  middlewares: [permissions],
  context: req => ({
    ...req,
    db: new Prisma({
      typeDefs: 'src/generated/prisma.graphql',
      endpoint: 'http://localhost:4466/graphql-authentication',
    }),
    graphqlAuthentication: graphqlAuthenticationConfig({
      adapter: new GraphqlAuthenticationPrismaAdapter(),
      secret: APP_SECRET,
      mailer,
      mailAppUrl: 'http://example.com',
    }),
  }),
});
server.start(() => console.log('Server is running on localhost:4000'));
