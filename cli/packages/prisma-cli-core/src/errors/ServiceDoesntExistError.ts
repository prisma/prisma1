export class ServiceDoesntExistError extends Error {
  constructor(service: string) {
    super(`The service ${service} doesn't exist`)
  }
}
