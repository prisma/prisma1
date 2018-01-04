import * as jwt from 'jsonwebtoken'
import { Graphcool } from 'graphcool-binding'

export interface Context {
  db: Graphcool,
  request: any
}

export interface User {
  id: string,
  name: string,
  bio: string,
  public_repos: string,
  public_gists: string
}

export function getUserId(ctx: Context) {
  const Authorization = ctx.request.get('Authorization')
  if (Authorization) {
    const token = Authorization.replace('Bearer ', '')
    const { userId } = jwt.verify(token, process.env.JWT_SECRET!) as {
      userId: string
    }
    return userId
  }

  throw new AuthError()
}

export class AuthError extends Error {
  constructor() {
    super('Not authorized')
  }
}
