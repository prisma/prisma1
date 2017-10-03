import { Command, flags, Flags } from 'graphcool-cli-engine'
import { InvalidProjectError } from '../../errors/InvalidProjectError'
import Docker from './Docker'

export default class Restart extends Command {
  static topic = 'local'
  static command = 'restart'
  static description = 'Restart an already initialized local Graphcool instance'
  async run() {
    const docker = new Docker(this.out, this.config)
    await docker.restart()
  }
}
