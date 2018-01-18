export class StageNotFound extends Error {
  constructor(name?: string) {
    if (name) {
      super(`Stage '${name}' could not be found in the local prisma.yml`)
    } else {
      super(`No stage provided and no default stage set`)
    }
  }
}
