const validator = require('validator')

module.exports = function(event) {
  console.log(event)

  const email = event.data.email

  const firstName = event.data.firstName
  const lastName = event.data.lastName

  // verify input
  if (!validator.isEmail(email)) {
    return { error: 'Please enter a valid email address.' }
  }

  if (firstName === '' || lastName === '') {
    return { error: 'Names can\'t be empty.' }
  }

  // generate full name and include in input parameters
  event.data.fullName = `${firstName} ${lastName}`

  return event
}
