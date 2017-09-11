export class InvalidProjectNameError extends Error {
  constructor (projectName: string) {
    super(`'${projectName}' is not a valid project name. It must begin with an uppercase letter.`)
  }
}