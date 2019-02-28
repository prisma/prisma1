export class RequiredFlagError extends Error {
  constructor(name: string) {
    super(`Missing required flag --${name}`)
  }
}
