import { Context, getUserId, AuthError } from '../../utils'

export const note = async (_, { id }, ctx: Context, info) => {
    const userId = getUserId(ctx)
    const hasPermission = await ctx.db.exists.Note({
        id,
        owner: { id: userId }
    })

    if (!hasPermission) {
        throw new AuthError()
    }

    return await ctx.db.query.note({ where: { id } })
}