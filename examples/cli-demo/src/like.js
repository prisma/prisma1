const fromEvent = require('graphcool-lib').fromEvent

module.exports = (event) => {
  console.log(event)

  const graphcool = fromEvent(event)
  const api = graphcool.api('simple/v1')

  const post = event.data.Post.node

  if (post && post.id && post.author.id) {
    console.log(`Author ${post.author.id} created a new post: ${post.id}.`)

    // create like for user and post
    return api.request(
      `mutation {
        createLike(postId: "${post.id}", likedById: "${post.author.id}") {
          id
        }
      }`
    )
  } else {
    console.log('Post created without an author.')
  }
}
