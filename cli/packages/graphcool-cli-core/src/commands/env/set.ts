import {Command, flags, Flags} from 'graphcool-cli-engine'

export default class SetEnv extends Command {
  static topic = 'env'
  static command = 'set'
  static description = 'Sets a project environment'
  static flags: Flags = {
    project: flags.string({
      char: 'p',
      description: 'Project ID',
      required: true
    }),
    env: flags.string({
      char: 'e',
      description: 'Environment Name',
      required: true
    }),
  }
  async run() {
    const {project, env} = this.flags

    this.env.set(env, project)
    this.env.save()
    this.out.log(`Environment ${env} set to ${project}`)
  }
}
