const { forwardTo } = require('graphcool-binding')

/*
 * This is a simple case of passing through a query resolver from the DB to the app
 *
 * In this example, `forwardTo` uses
 *   - `context['db']`,
 *   - `info.parentType` (Query)
 *   - and `info.fieldName` (posts)
 * to forward the `posts` query to the database
 */
const Query = {
  posts: forwardTo('db')
}

module.exports = { Query }
