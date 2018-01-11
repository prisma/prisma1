const fromEvent = require('graphcool-lib').fromEvent

const userQuery = `
query UserQuery($userId: ID!) {
  User(id: $userId){
    id
    password
  }
}`

const getUser = (api, userId) => {
  return api.request(userQuery, { userId })
    .then(userQueryResult => {
      return userQueryResult.User
    })
    .catch(error => {
      // Log error, but don't expose to caller
      console.log(`Error: ${JSON.stringify(error)}`)
      return { error: `An unexpected error occured` }
    })
}

module.exports = event => {

  if (!event.context.auth || !event.context.auth.nodeId) {
    console.log(`No auth context`)
    return {data: {id: null}}
  }

  // Retrieve payload from event
  const userId = event.context.auth.nodeId
  console.log(`Node ID: ${userId}`)

  // Create Graphcool API (based on https://github.com/graphcool/graphql-request)  
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  return getUser(api, userId)
    .then(emailUser => {
      if (!emailUser) {
        return { error: `No user with id: ${userId}` }
      }
      return { data: emailUser }
    })
    .catch(error => {
      // Log error, but don't expose to caller
      console.log(`Error: ${JSON.stringify(error)}`)
      return { error: `An unexpected error occured` }
    })

}