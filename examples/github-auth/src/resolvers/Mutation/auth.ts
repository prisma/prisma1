import { Context, User } from '../../utils'
import { getGithubToken, getGithubUser, GithubUser } from '../../github'
import * as jwt from 'jsonwebtoken'

// Helpers -------------------------------------------------------------------

async function getPrismaUser(ctx: Context, githubUserId: string): Promise<User> {
  return await ctx.db.query.user({ where: { githubUserId } })
}

async function createPrismaUser(ctx, githubUser: GithubUser): Promise<User> {
  const user = await ctx.db.mutation.createUser({
    data: {
      githubUserId: githubUser.id,
      name: githubUser.name,
      bio: githubUser.bio || 'User has no bio',
      public_repos: githubUser.public_repos,
      public_gists: githubUser.public_gists,
      notes: [],
    },
  })
  return user
}

// Resolvers -----------------------------------------------------------------

export const auth = {
  authenticate: async (parent, { githubCode }, ctx: Context, info) => {
    const githubToken = await getGithubToken(githubCode)
    const githubUser = await getGithubUser(githubToken)

    let user = await getPrismaUser(ctx, githubUser.id)

    if (!user) {
      user = await createPrismaUser(ctx, githubUser)
    }
    console.log(`app secret`, process.env.APP_SECRET)
    return {
      token: jwt.sign({ userId: user.id }, process.env.APP_SECRET),
      user,
    }
  },
}
