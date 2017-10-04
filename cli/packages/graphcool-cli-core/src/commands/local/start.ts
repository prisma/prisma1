import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidProjectError'
import Docker from './Docker'

export default class Start extends Command {
  static topic = 'local'
  static command = 'start'
  static description = 'Start an already initialized local Graphcool instance'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment to start',
    }),
  }
  async run() {
    const docker = new Docker(this.out, this.config)
    await docker.start()
  }
}
