import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidProjectError'
import Docker from './Docker'

export default class Stop extends Command {
  static topic = 'local'
  static command = 'stop'
  static description = 'Stop an already initialized local Graphcool instance'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment to stop',
    }),
  }
  async run() {
    const docker = new Docker(this.out, this.config)
    await docker.stop()
  }
}
