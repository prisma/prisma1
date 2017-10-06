import { Command, flags, Flags, AuthenticateCustomerPayload } from 'graphcool-cli-engine'
import Docker from './Docker'
import * as chalk from 'chalk'
import * as util from 'util'

export default class Up extends Command {
  static topic = 'local'
  static command = 'up'
  static description = 'Pull & Start a local Graphcool instance'
  static flags: Flags = {
    env: flags.string({
      char: 'e',
      description: 'Environment name for the new instance',
    }),
  }
  async run() {
    this.config.setLocal()
    this.client.updateClient()

    let {env} = this.flags

    env = env || 'local-dev'
    if (this.env.env.environments[env]) {
      this.out.error(`The environment ${chalk.bold(env)} already exists. Please provide a new one with ${chalk.green('--env NEW_NAME')}`)
    }

    const docker = new Docker(this.out, this.config)

    const {MASTER_TOKEN, PORT} = await docker.up()

    this.out.log('')
    this.out.action.start('Waiting for Graphcool to initialize. This can take several minutes')
    await this.client.waitForLocalDocker()

    const {token}: AuthenticateCustomerPayload = await this.client.authenticateCustomer(MASTER_TOKEN)
    this.env.setDockerEnv(env, {
      token,
      host: `http://localhost:${PORT}`,
      projectId: null
    })
    this.out.action.stop()

    if (!this.env.default) {
      this.env.setDefault(env)
    }
    this.env.save()

    this.out.log(`\nSuccess! Added local environment ${chalk.dim(`\`${env}\``)} to .graphcoolrc\n`)
    this.out.log(`To get started, execute
    
  ${chalk.green('$ graphcool init')}
  ${chalk.green('$ graphcool deploy')}
`)
  }
}
