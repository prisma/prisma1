const { GraphQLServer } = require("graphql-yoga");
const { Prisma, forwardTo } = require("prisma-binding");

const resolvers = {
  Query: {
    posts: forwardTo("db")
  },

  Mutation: {
    createPost: forwardTo("db"),
    deletePost: forwardTo("db")
  }
};

const server = new GraphQLServer({
  typeDefs: "schema.graphql",
  resolvers,
  context: {
    db: new Prisma({
      typeDefs: "generated-schema.graphql",
      endpoint: "http://localhost:4466/resolver-forwarding"
    }),
  }
});
server.start(() => console.log("Server is running on localhost:4000"));
