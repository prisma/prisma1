import { Command } from 'graphcool-cli-engine'
import Docker from './Docker'

export default class PullLocal extends Command {
  static topic = 'local'
  static command = 'pull'
  static description = 'Pull the latest Graphcool version'
  async run() {
    const docker = new Docker(this.out, this.config)
    await docker.pull()
  }
}
