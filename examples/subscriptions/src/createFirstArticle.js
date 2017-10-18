const { fromEvent } = require('graphcool-lib')

// The input type for the function is determined by the subscription query
// defined in `createFirstArticle.graphql`.
// A sample payload could look like this:
// 
// event = {
//   "data": {
//     "User": {
//       "node": {
//         "name": "Sarah"
//       }
//     }
//   },
//   // more event data ...
// }

module.exports = event => {

  // Retrieve payload from event
  const { name } = event.data.User.node

  // Create Graphcool API (based on https://github.com/graphcool/graphql-request)
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  // Create variables for mutation
  const title = `My name is ${name}, and this is my first article! 🙌`
  const variables = { title }

  // Create mutation
  const createArticleMutation = `
    mutation ($title: String!) {
      createArticle(title: $title) {
        id
      }
    }
  `

  // Send mutation with variables
  return api.request(createArticleMutation, variables)
}