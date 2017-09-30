const Pusher = require('pusher')

module.exports = event => {
  console.log(event)

  if (!process.env['PUSHER_APP_ID'] || !process.env['PUSHER_KEY'] || !process.env['PUSHER_SECRET'] || !process.env['PUSHER_CLUSTER']) {
    console.log('Pusher environment variables are missing. Did you add them to graphcool.yml?')
    return { error: 'Module not configured correctly.' }
  }

  const pusher = new Pusher({
    appId: process.env['PUSHER_APP_ID'],
    key: process.env['PUSHER_KEY'],
    secret: process.env['PUSHER_SECRET'],
    cluster: process.env['PUSHER_CLUSTER'],
    encrypted: true
  })

  // publish new notification for created post
  const pusherChannel = 'posts'
  const pusherEvent = 'created'
  const pusherMessage = `Read this new post: '${event.data.Post.node.title}'!`

  console.log(pusherMessage)

  return new Promise((resolve, reject) => {
    pusher.trigger(
      pusherChannel,
      pusherEvent, {
        'message': pusherMessage
      },
      (error, request, response) => {
        if (error) {
          console.log('Event could not be pushed.')
          console.log(error)
          return reject()
        } else {
          console.log(response)

          return resolve()
        }
      }
    )
  })
  .catch(error => {
    console.log(error.toString())
    return
  })
}
