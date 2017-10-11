import { Command, flags, Flags, AuthenticateCustomerPayload } from 'graphcool-cli-engine'
import Docker from './Docker'
import * as chalk from 'chalk'
const debug = require('debug')('up')

export default class Up extends Command {
  static topic = 'local'
  static command = 'up'
  static description = 'Start local development cluster (Docker required)'
  static group = 'local'
  static flags: Flags = {
    name: flags.string({
      char: 'n',
      description: 'Name of the new instance',
      defaultValue: 'local'
    }),
  }
  async run() {
    const {name} = this.flags

    const docker = new Docker(this.out, this.config, this.env, name)

    const {envVars: {MASTER_TOKEN, PORT}} = await docker.up()

    this.out.log('')
    this.out.action.start('Waiting for Graphcool to initialize. This can take several minutes')
    const cluster = (this.env.rc.clusters && this.env.rc.clusters[name]) ? this.env.rc.clusters[name] : null
    const host = (cluster && typeof cluster !== 'string') ? cluster.host : 'http://localhost:' + PORT
    const endpoint = host + '/system'
    await this.client.waitForLocalDocker(endpoint)

    const {token}: AuthenticateCustomerPayload = await this.client.authenticateCustomer(endpoint, MASTER_TOKEN)


    if (!cluster) {
      debug('Setting cluster')
      this.env.setGlobalCluster(name, {
        host,
        token,
      })
      this.env.saveGlobalRC()
    }

    this.out.action.stop()

    if (!cluster) {
      this.out.log(`\nSuccess! Added local cluster ${chalk.bold(`\`${name}\``)} to ${this.config.globalRCPath}\n`)
    } else {
      this.out.log(`\nSuccess! Cluster ${name} already exists and is up-to-date.`)
    }
    this.out.log(`To get started, execute
    
  ${chalk.green('$ graphcool init')}
  ${chalk.green('$ graphcool deploy')}
`)
  }
}
