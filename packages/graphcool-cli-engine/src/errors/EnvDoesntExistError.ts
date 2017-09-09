export class EnvDoesntExistError extends Error {
  constructor (env: string) {
    super(`The environment ${env} doesn't exist in the local .graphcoolrc`)
  }
}