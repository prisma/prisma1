export class InvalidProjectError extends Error {
  constructor () {
    super(`Please provide a valid environment that has a valid project id`)
  }
}