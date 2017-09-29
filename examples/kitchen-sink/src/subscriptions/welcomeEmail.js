const fromEvent = require('graphcool-lib').fromEvent

module.exports = function(event) {
  console.log(event)

  if (!process.env['SENDER_EMAIL']) {
    console.log('Please provide a valid sender email!')
    return { error: 'Welcome email not configured correctly!' }
  }

  const email = event.data.Contact.node.email
  const name = event.data.Contact.node.fullName

  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  const mutation = `
    mutation sendEmail(
      $tag: String!
      $from: String!
      $to: String!
      $subject: String!
      $text: String!
    ) {
      sendMailgunEmail(
        tag: $tag
        from: $from
        to: $to
        subject: $subject
        text: $text
      ) {
        success
      }
    }
  `

  const variables = {
    tag: `${new Date().toISOString().slice(0, 10)}-welcome`,
    from: process.env['SENDER_EMAIL'],
    to: email,
    subject: `Hey, ${name}!`,
    text: 'Welcome!',
  }

  return api.request(mutation, variables)
    .then(response => {
      console.log(response)

      return {}
    })
    .catch(error => {
      console.log(error)

      return {}
    })
}
