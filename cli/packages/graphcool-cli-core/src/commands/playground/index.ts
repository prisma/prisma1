import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as opn from 'opn'
import { playgroundURL } from '../../util'
import { InvalidProjectError } from '../../errors/InvalidProjectError'
import * as fs from 'fs-extra'
import * as childProcess from 'child_process'

export default class Playground extends Command {
  static topic = 'playground'
  static description = 'Open the playground for the current selected project'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name',
    }),
  }
  async run() {
    // await this.auth.ensureAuth()
    let { env } = this.flags

    env = env || this.env.env.default

    const { projectId } = await this.env.getEnvironment({ env })
    let host = undefined

    if (this.env.env && this.env.isDockerEnv(this.env.env.environments[env])) {
      host = (this.env.env.environments[env] as any).host
    }

    if (!projectId) {
      this.out.error(new InvalidProjectError())
    } else {
      const localPlaygroundPath = `/Applications/GraphQL\ Playground.app/Contents/MacOS/GraphQL\ Playground`

      if (fs.pathExistsSync(localPlaygroundPath)) {
        childProcess.spawn(
          localPlaygroundPath,
          ['endpoint', playgroundURL(projectId, host)],
          { detached: true },
        )
      } else {
        opn(playgroundURL(projectId, host))
      }
    }
  }
}
