import { Context, User, getUserId } from '../../utils'

export const me = async (_, args, ctx: Context, info) => {
    const id = getUserId(ctx)
    return await ctx.db.query.user({ where: { id }}, info)
}
