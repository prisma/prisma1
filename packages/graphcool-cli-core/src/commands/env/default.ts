import {Command, flags, Flags} from 'graphcool-cli-engine'

export default class DefaultEnv extends Command {
  static topic = 'env'
  static command = 'default'
  static description = 'Set the default environment'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Env to set as default',
      required: true
    }),
  }
  async run() {
    const {env} = this.flags

    this.env.setDefault(env)
    this.env.save()
    this.out.log(`Successfully set ${env} as the default environment`)
  }
}
