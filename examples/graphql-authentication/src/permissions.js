const { rule, shield } = require('graphql-shield');
const { isAuthResolver } = require('graphql-authentication');

const isAuthenticated = rule()(isAuthResolver);

const permissions = shield({
  Mutation: {
    createPost: isAuthenticated,
  },
});

module.exports = { permissions };
