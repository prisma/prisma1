export class ProjectDoesntExistError extends Error {
  constructor (project: string) {
    super(`The project ${project} doesn't exist`)
  }
}