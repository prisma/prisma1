import { Command } from 'graphcool-cli-engine'
import { table, getBorderCharacters } from 'table'

export default class List extends Command {
  static topic = 'list'
  static description = 'List all deployed services'
  static group = 'general'
  async run() {
    await this.auth.ensureAuth()
    const projects = await this.client.fetchProjects()
    this.out.log(this.out.printServices(this.env.rc.targets!, projects))
  }
}
