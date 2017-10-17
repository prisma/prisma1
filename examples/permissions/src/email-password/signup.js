const fromEvent = require('graphcool-lib').fromEvent
const bcrypt = require('bcryptjs')
const validator = require('validator')

function getGraphcoolUser(api, email) {
  return api.request(`
    query {
      User(email: "${email}") {
        id
      }
    }`)
    .then((userQueryResult) => {
      if (userQueryResult.error) {
        return Promise.reject(userQueryResult.error)
      } else {
        return userQueryResult.User
      }
    })
}

function createGraphcoolUser(api, email, passwordHash, admin) {
  return api.request(`
    mutation {
      createUser(
        email: "${email}",
        password: "${passwordHash}",
        role: ${admin ? 'ADMIN' : 'CUSTOMER'}
      ) {
        id
      }
    }`)
    .then((userMutationResult) => {
      return userMutationResult.createUser.id
    })
}

module.exports = function(event) {
  if (!event.context.graphcool.pat) {
    console.log('Please provide a valid root token!')
    return { error: 'Email Signup not configured correctly.'}
  }

  const email = event.data.email
  const password = event.data.password
  const admin = event.data.admin || false
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')
  const SALT_ROUNDS = 10
  const salt = bcrypt.genSaltSync(SALT_ROUNDS)

  if (validator.isEmail(email)) {
    return getGraphcoolUser(api, email)
      .then(graphcoolUser => {
        if (graphcoolUser === null) {
          return bcrypt.hash(password, salt)
            .then(hash => createGraphcoolUser(api, email, hash, admin))
        } else {
          return Promise.reject("Email already in use")
        }
      })
      .then(graphcoolUserId => {
        return graphcool.generateAuthToken(graphcoolUserId, 'User')
          .then(token => {
            return { data: {id: graphcoolUserId, token}}
        })
      })
      .catch((error) => {
        console.log(error)

        // don't expose error message to client!
        return { error: 'An unexpected error occured.' }
      })
  } else {
    return { error: "Not a valid email" }
  }
}
