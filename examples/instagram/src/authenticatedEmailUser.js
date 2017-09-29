const fromEvent = require('graphcool-lib').fromEvent

function getEmailUser(api, userId) {
  return api.request(`
    query {
      EmailUser(id: "${userId}"){
        id
      }
    }`)
    .then(userQueryResult => {
      console.log(userQueryResult)
      return userQueryResult.EmailUser
    })
    .catch(error => {
      // Log error but don't expose to caller
      console.log(error)
      return { error: `An unexpected error occured` }
    })
}

module.exports = function authenticatedEmailUser(event) {

  if (!event.context.auth || !event.context.auth.nodeId) {
    return {data: {id: null}}
  }

  const userId = event.context.auth.nodeId
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  return getEmailUser(api, userId)
    .then(emailUser => {
      if (!emailUser) {
        return { error: `No user with id: ${userId}` }
      }
      return { data: emailUser }
    })
    .catch(error => {
      // Log error but don't expose to caller
      console.log(error)
      return { error: `An unexpected error occured` }
    })

}