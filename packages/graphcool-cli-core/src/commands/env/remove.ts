import {Command, flags, Flags} from 'graphcool-cli-engine'

export default class RemoveEnv extends Command {
  static topic = 'env'
  static command = 'remove'
  static description = 'Removes a project environment'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Env to remove',
      required: true
    }),
  }
  async run() {
    const {env} = this.flags

    this.env.load()
    this.env.remove(env)
    this.env.save()
    this.out.log(`Removed environment ${env}`)
  }
}
