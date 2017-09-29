const fetch = require('isomorphic-fetch')
const FormData =require('form-data')

module.exports = event => {
  console.log(event)

  const token = new Buffer(`api:${process.env['MAILGUN_API_KEY']}`).toString('base64')
  const endpoint = `https://api.mailgun.net/v3/${process.env['MAILGUN_DOMAIN']}/messages`

  const tag = event.data.tag
  const from = event.data.from
  const to = event.data.to
  const subject = event.data.subject
  const text = event.data.text
  const recipientVariables = event.data.recipientVariables || {}

  if (to.length > 1000) {
    console.log(`Can't batch more than 1000 emails!`)
    return { error: `Can't batch more than 1000 emails!` }
  }

  const form = new FormData()
  form.append('from', from)

  for (var i = 0; i < to.length; i++) {
    form.append('to', to[i])
  }

  form.append('subject', subject)
  form.append('text', text)
  form.append('recipient-variables', JSON.stringify(recipientVariables))

  return fetch(endpoint, {
    headers: {
      'Authorization': `Basic ${token}`
    },
    method: 'POST',
    body: form
  })
  .then(response => response.json())
  .then(json => {
    console.log(`Email both valid, and queued to be delivered.`)
    console.log(json)

    return { data: { success: true } }
  })
  .catch(error => {
    console.log(`Email ${id} could not be sent, because an error occured:`)
    console.log(error)

    return { data: { success: false } }
  })
}
