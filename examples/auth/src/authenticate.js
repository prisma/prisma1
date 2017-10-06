const fromEvent = require('graphcool-lib').fromEvent
const bcrypt = require('bcrypt')

const userQuery = `
query UserQuery($email: String!) {
  EmailUser(email: $email){
    id
    password
  }
}`

const getGraphcoolUser = (api, email) => {
  return api.request(userQuery, { email })
    .then(userQueryResult => {
      if (userQueryResult.error) {
        return Promise.reject(userQueryResult.error)
      } else {
        return userQueryResult.EmailUser
      }
    })
}

module.exports = event => {
  if (!event.context.graphcool.pat) {
    console.log('Please provide a valid root token!')
    return { error: 'Email Authentication not configured correctly.'}
  }

  // Retrieve payload from event
  const email = event.data.email
  const password = event.data.password

  // Create Graphcool API (based on https://github.com/graphcool/graphql-request)
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  return getGraphcoolUser(api, email)
    .then(graphcoolUser => {
      if (!graphcoolUser) {
        return Promise.reject("Invalid Credentials") //returning same generic error so user can't find out what emails are registered.
      } else {
        return bcrypt.compare(password, graphcoolUser.password)
          .then(passwordCorrect => {
            if (passwordCorrect) {
              return graphcoolUser.id
            } else {
              return Promise.reject("Invalid Credentials")
            }
          })
      }
    })
    .then(graphcoolUserId => {
      return graphcool.generateAuthToken(graphcoolUserId, 'EmailUser')
    })
    .then(token => {
      return { data: { token } }
    })
    .catch(error => {
      // Log error, but don't expose to caller
      console.log(`Error: ${JSON.stringify(error)}`)
      return { error: `An unexpected error occured` }
    })
}
