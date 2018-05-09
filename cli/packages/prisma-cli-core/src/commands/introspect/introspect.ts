import { Command, flags, Flags } from 'prisma-cli-engine'
import { EndpointDialog } from '../../utils/EndpointDialog'
import { Introspector } from 'prisma-db-introspection'
import * as path from 'path'
import * as fs from 'fs'
import { prettyTime } from '../../util'
import chalk from 'chalk'

export default class IntrospectCommand extends Command {
  static topic = 'introspect'
  static description = 'Introspect database schema(s) of service'
  static flags: Flags = {}
  static hidden = false
  async run() {
    await this.definition.load(this.flags)

    const endpointDialog = new EndpointDialog(
      this.out,
      this.client,
      this.env,
      this.config,
    )

    const credentials = await endpointDialog.getDatabase(true)

    const introspector = new Introspector(credentials)
    let schemas
    const before = Date.now()
    this.out.action.start(`Introspecting database`)
    try {
      schemas = await introspector.listSchemas()
    } catch (e) {
      throw new Error(`Could not connect to database. ${e.message}`)
    }
    if (schemas && schemas.length > 0) {
      const { sdl, numTables } = await introspector.introspect(schemas[0])
      if (numTables === 0) {
        this.out.log(
          chalk.red(
            `\n${chalk.bold(
              'Error: ',
            )}The provided database doesn't contain any tables. Please provide another database.`,
          ),
        )
        this.out.exit(1)
      }
      const fileName = `datamodel-${new Date().getTime()}.graphql`
      const fullFileName = path.join(this.config.definitionDir, fileName)
      fs.writeFileSync(fullFileName, sdl)
      this.out.action.stop(prettyTime(Date.now() - before))
      this.out.log(
        `Created datamodel definition based on ${numTables} database tables.`,
      )
      this.out.log(`\
${chalk.bold(
        'Created 1 new file:',
      )}    GraphQL SDL-based datamodel (derived from existing database)

  ${chalk.cyan(fileName)}
`)
    } else {
      throw new Error(`Could not find schema in provided database.`)
    }
  }
}
