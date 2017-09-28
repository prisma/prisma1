export class EnvAlreadyExistsError extends Error {
  constructor (environments: string[]) {
    super(`You already have a project environment set up with the environments "${environments.join(', ')}".
  In order to create a new project, either do that in a seperate folder or add it to the current environments with
  providing the --env option.`)
  }
}
