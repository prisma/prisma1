const resolvers = require('../resolvers')
const { getPrismaTestInstance } = require('../util')

afterEach(async () => {
  await getPrismaTestInstance().mutation.deleteManyPosts({})
})

test('creating draft works', async () => {
  const parent = {}
  const args = {
    data: {
      content: 'some text',
      status: 'DRAFT',
      title: 'New post',
    },
  }
  const ctx = {
    db: getPrismaTestInstance(),
  }

  const info = `{ content, status, title }`

  expect(
    await resolvers.Mutation.createPost(parent, args, ctx, info),
  ).toMatchSnapshot()
})

test('creating publish fails', async () => {
  try {
    const parent = {}
    const args = {
      data: {
        content: 'some text',
        status: 'PUBLISH',
        title: 'New post',
      },
    }
    const ctx = {
      db: getPrismaTestInstance(),
    }

    const info = `{ id }`
    const result = await resolvers.Mutation.createPost(parent, args, ctx, info)
    expect(0).toBe(1)
  } catch (e) {
    expect(e.toString()).toMatchSnapshot()
  }
})
