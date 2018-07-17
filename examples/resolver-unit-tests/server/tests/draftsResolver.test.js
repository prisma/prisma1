const resolvers = require('../resolvers')
const { getPrismaTestInstance } = require('../util')

afterEach(async () => {
  await getPrismaTestInstance().mutation.deleteManyPosts({})
})

test('querying drafts works', async () => {
  // create new draft
  await getPrismaTestInstance().mutation.createPost({
    data: {
      title: 'new draft',
      content: 'tbd',
      status: 'DRAFT',
    },
  })

  // query drafts
  expect(
    await resolvers.Query.drafts(
      {},
      {},
      { db: getPrismaTestInstance() },
      `{ status }`,
    ),
  ).toMatchSnapshot()
})
