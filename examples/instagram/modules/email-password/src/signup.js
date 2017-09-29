const fromEvent = require('graphcool-lib').fromEvent
const bcrypt = require('bcrypt')
const validator = require('validator')

function getGraphcoolUser(api, email) {
  return api.request(`
    query {
      EmailUser(email: "${email}") {
        id
      }
    }`)
    .then((userQueryResult) => {
      if (userQueryResult.error) {
        return Promise.reject(userQueryResult.error)
      } else {
        return userQueryResult.EmailUser
      }
    })
}

function createGraphcoolUser(api, email, passwordHash, name) {
  return api.request(`
    mutation {
      createEmailUser(
        email: "${email}",
        password: "${passwordHash}",
        name: "${name}"
      ) {
        id
      }
    }`)
    .then((userMutationResult) => {
      return userMutationResult.createEmailUser.id
    })
}

module.exports = function(event) {
  const email = event.data.email
  const password = event.data.password
  const name = event.data.name
  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')
  const SALT_ROUNDS = 10

  if (validator.isEmail(email)) {
    return getGraphcoolUser(api, email)
      .then((graphcoolUser) => {
        if (graphcoolUser === null) {
          return bcrypt.hash(password, SALT_ROUNDS)
            .then(hash => createGraphcoolUser(api, email, hash, name))
        } else {
          return Promise.reject("Email already in use")
        }
      })
      .then((id) => {
        return { data: { id } }
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
