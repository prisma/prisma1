import { Command, flags, Flags, AuthenticateCustomerPayload } from 'graphcool-cli-engine'
import Docker from './Docker'
import * as chalk from 'chalk'
const debug = require('debug')('up')

export default class Up extends Command {
  static topic = 'local'
  static command = 'up'
  static description = 'Pull & Start a local Graphcool instance'
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Name of the new instance',
      defaultValue: 'dev'
    }),
  }
  async run() {
    this.config.setLocal()
    this.client.updateClient()

    const {name} = this.flags

    const docker = new Docker(this.out, this.config, name)

    const {envVars: {MASTER_TOKEN, PORT}} = await docker.up()

    this.out.log('')
    this.out.action.start('Waiting for Graphcool to initialize. This can take several minutes')
    await this.client.waitForLocalDocker()

    const {token}: AuthenticateCustomerPayload = await this.client.authenticateCustomer(MASTER_TOKEN)


    if (!this.config.targets[name]) {
      debug('Setting target')
      this.config.setTarget(name, {
        description: 'This is your local docker dev environment',
        host: `http://localhost:${PORT}`,
        token,
      })
      this.config.saveDotGraphcool()
    }

    this.out.action.stop()

    this.out.log(`\nSuccess! Added local instance ${chalk.bold(`\`${name}\``)} to ${this.config.dotGraphcoolFilePath}\n`)
    this.out.log(`To get started, execute
    
  ${chalk.green('$ graphcool init')}
  ${chalk.green('$ graphcool deploy')}
`)
  }
}
