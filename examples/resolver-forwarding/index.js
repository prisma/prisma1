const { GraphQLServer } = require("graphql-yoga");
const { Prisma, forwardTo } = require("prisma-binding");

const getPrismaInstance = () => {
  return new Prisma({
    typeDefs: "generated-schema.graphql",
    endpoint: "http://localhost:4466/resolver-forwarding"
  });
};

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
    db: getPrismaInstance()
  }
});
server.start(() => console.log("Server is running on localhost:4000"));
