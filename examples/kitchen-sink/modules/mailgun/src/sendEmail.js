const fetch = require('isomorphic-fetch')
const FormData =require('form-data')

module.exports = event => {
  console.log(event)

  if (!process.env['MAILGUN_API_KEY']) {
    console.log('Please provide a valid mailgun secret key!')
    return { error: 'Module not configured correctly.' }
  }

  if (!process.env['MAILGUN_DOMAIN']) {
    console.log('Please provide a valid mailgun domain!')
    return { error: 'Module not configured correctly.' }
  }

  const token = new Buffer(`api:${process.env['MAILGUN_API_KEY']}`).toString('base64')
  const endpoint = `https://api.mailgun.net/v3/${process.env['MAILGUN_DOMAIN']}/messages`

  const tag = event.data.tag
  const from = event.data.from
  const subject = event.data.subject
  const text = event.data.text
  const recipientVariables = event.data.recipientVariables || {}

  // workaround for https://github.com/graphcool/graphcool/issues/568
  let to = event.data.to
  if (typeof to === 'string') {
    to = [to]
  }

  if (to.length > 1000) {
    console.log(`Can't batch more than 1000 emails!`)
    return { error: `Can't batch more than 1000 emails!` }
  }

  if (to.length === 1) {
    console.log('Sending out email:')
    console.log(`[${tag} - ${subject}] from ${from} to ${to}`)
  } else {
    console.log('Sending out batched email:')
    console.log(`[${tag} - ${subject}] from ${from}`)
    for (var i = 0; i < to.length; i++) {
      console.log(`recipients: ${to[i]}`)
    }
    console.log(`recipientVariables: ${recipientVariables}`)
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
    console.log(`Email ${id} could not be sent because an error occured:`)
    console.log(error)

    return { data: { success: false } }
  })
}
