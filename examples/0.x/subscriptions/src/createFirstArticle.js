const { fromEvent } = require('graphcool-lib')

// The input type for the function is determined by the subscription query
// defined in `createFirstArticle.graphql`.
// A sample payload could look like this:
// 
// event = {
//   "data": {
//     "User": {
//       "node": {
//         "id": "cj8wscby6nl7u0133zu7c8a62"
//         "name": "Sarah"
//       }
//     }
//   },
//   // more event data ...
// }

module.exports = event => {

  // Retrieve payload from event
  const { id, name } = event.data.User.node

  // Create Graphcool API (based on https://github.com/graphcool/graphql-request)
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  // Create variables for mutation
  const title = `My name is ${name}, and this is my first article!`
  const variables = { authorId: id, title }

  // Create mutation
  const createArticleMutation = `
    mutation ($title: String!, $authorId: ID!) {
      createArticle(title: $title, authorId: $authorId) {
        id
      }
    }
  `

  // Send mutation with variables
  return api.request(createArticleMutation, variables)
}