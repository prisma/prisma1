import { Context, getUserId, AuthError } from '../../utils'

export const notes = {
    async createNote(_, { text }, ctx: Context, info) {
        const userId = getUserId(ctx)
        return await ctx.db.mutation.createNote({ data: {
            owner: { connect: { id: userId } },
            text
        }})
    },
    async updateNote(_, { id, text }, ctx: Context, info) {
        const userId = getUserId(ctx)
        const hasPermission = await ctx.db.exists.Note({
            id,
            owner: { id: userId }
        })

        if (!hasPermission) {
            throw new AuthError()
        }

        return await ctx.db.mutation.updateNote({
            where: { id },
            data: { text }
        })
    },
    async deleteNote(_, { id }, ctx: Context, info) {
        const userId = getUserId(ctx)
        const hasPermission = await ctx.db.exists.Note({
            id,
            owner: { id: userId }
        })

        if (!hasPermission) {
            throw new AuthError()
        }
        return await ctx.db.mutation.deleteNote({ 
            where: { id }
        })
    }
}


