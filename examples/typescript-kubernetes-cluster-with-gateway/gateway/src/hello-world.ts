export const typeDefs = `
  extend type Query {
    hello: Hello!
  }
  
  type Hello {
    message: String!
  }
`

export const resolver = (mergeInfo) => ({
  Query: {
    /*
      We proxy (delegate) to the actual graphcool api (running in our cluster)
      utilizing what we get from the apollo-server parameters and the mergeInfo
      parameter from each custom resolver.

      return mergeInfo.delegate('query', 'Type', *CONTEXT*, *INFO*)
    */
    hello: (_parent, _args, _context, _info) => ({ message: 'World!' }),
  }
})
