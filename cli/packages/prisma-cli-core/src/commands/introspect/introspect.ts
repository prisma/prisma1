import { Command, flags, Flags } from 'prisma-cli-engine'
import { EndpointDialog } from '../../utils/EndpointDialog'
import { Introspector } from 'prisma-db-introspection'
import * as path from 'path'
import * as fs from 'fs'

export default class IntrospectCommand extends Command {
  static topic = 'introspect'
  static description = 'Introspect database schema(s) of service'
  static flags: Flags = {}
  static hidden = true
  async run() {
    await this.definition.load(this.flags)

    const endpointDialog = new EndpointDialog(
      this.out,
      this.client,
      this.env,
      this.config,
    )

    const credentials = await endpointDialog.getDatabase()

    const introspector = new Introspector(credentials)
    let schemas
    try {
      schemas = await introspector.listSchemas()
    } catch (e) {
      throw new Error(`Could not connect to database. ${e.message}`)
    }
    if (schemas && schemas.length > 0) {
      const datamodel = await introspector.introspect(schemas[0])
      fs.writeFileSync(
        path.join(this.config.definitionDir, 'datamodel.graphql'),
        datamodel,
      )
      this.out.log(`Wrote new introspected schema to datamodel.graphql`)
    } else {
      throw new Error(`Could not find schema in provided database.`)
    }
  }
}
