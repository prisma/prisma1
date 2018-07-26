const { Prisma } = require('prisma-binding')

const getPrismaTestInstance = () => {
  return new Prisma({
    typeDefs: 'src/generated/prisma.graphql',
    endpoint: 'http://localhost:4466/travis-jest/test',
    secret: 'my-very-secret',
  })
}
module.exports = {
  getPrismaTestInstance,
}
