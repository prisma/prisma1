export default class ExitError extends Error {
  code: number

  constructor(code: number) {
    super(`Exited with code: ${code}`)
    this.code = code
  }
}
