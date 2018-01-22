import { Command, flags, Flags } from 'prisma-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'
import * as npmRun from 'npm-run'
const debug = require('debug')('init')
import * as spawn from 'cross-spawn'

export default class Init extends Command {
  static topic = 'init'
  static description = 'Initialize a new service'

  static args = [
    {
      name: 'dirName',
      description: 'Folder to initialize in (optional)',
    },
  ]

  static flags: Flags = {
    boilerplate: flags.string({
      char: 'b',
      description:
        'Full URL or repo shorthand (e.g. `owner/repo`) to boilerplate GitHub repository',
    }),
  }

  async run() {
    const dirName = this.args!.dirName
    if (dirName) {
      const newDefinitionDir = path.join(process.cwd(), dirName + '/')
      this.config.definitionDir = newDefinitionDir
      fs.mkdirpSync(newDefinitionDir)
    } else {
      this.config.definitionDir = process.cwd()
    }

    const { boilerplate } = this.flags
    if (boilerplate) {
      await this.graphqlCreate(boilerplate)
    } else {
      const choice = await this.prompt()

      if (choice === 'create') {
        await this.graphqlCreate()
      } else {
        this.initMinimal()
      }
    }
  }

  async prompt(): Promise<'create' | 'minimal'> {
    const question = {
      name: 'prompt',
      type: 'list',
      message: `How to set up a new Prisma service?`,
      choices: [
        {
          name: 'Minimal setup: database-only',
          value: 'minimal',
        },
        {
          name: 'GraphQL server/fullstack boilerplate (recommended)',
          value: 'create',
        },
      ],
      pageSize: 2,
    }

    const { prompt } = await this.out.prompt(question)

    this.out.up(1)

    return prompt
  }

  initMinimal() {
    const files = fs.readdirSync(this.config.definitionDir)
    // the .prismarc must be allowed for the docker version to be functioning
    // CONTINUE: special env handling for dockaa. can't just override the host/dinges
    if (
      files.length > 0 &&
      (files.includes('prisma.yml') || files.includes('datamodel.graphql'))
    ) {
      this.out.log(`
The directory ${chalk.cyan(
        this.config.definitionDir,
      )} contains files that could conflict:

${files.map(f => `  ${f}`).join('\n')}

Either try using a new directory name, or remove the files listed above.
      `)
      this.out.exit(1)
    }

    fs.copySync(path.join(__dirname, 'boilerplate'), this.config.definitionDir)
    let relativeDir = path.relative(process.cwd(), this.config.definitionDir)
    relativeDir = relativeDir.length === 0 ? '.' : relativeDir

    const definitionPath = path.join(this.config.definitionDir, 'prisma.yml')
    const prismaYml = fs.readFileSync(definitionPath, 'utf-8')

    const newPrismaYml = prismaYml.replace(
      'SERVICE_NAME',
      path.basename(this.config.definitionDir),
    )
    fs.writeFileSync(definitionPath, newPrismaYml)

    const dir = this.args!.dirName
    const dirString = dir
      ? `Open the new folder via ${chalk.cyan(`$ cd ${dir}`)}. `
      : ``

    this.out.log(`\
Created 3 new files:               

  ${chalk.cyan('prisma.yml')}           Prisma service definition
  ${chalk.cyan(
    'datamodel.graphql',
  )}    GraphQL SDL-based datamodel (foundation for database)
  ${chalk.cyan(
    '.graphqlconfig.yml',
  )}   Configuration file for GraphQL tooling (Playground, IDE, …)

${dirString}You can now run ${chalk.cyan(
      '$ prisma deploy',
    )} to deploy your database service.

For next steps follow this tutorial: https://bit.ly/prisma-graphql-first-steps`)
  }
  async graphqlCreate(boilerplate?: string) {
    this.out.log(
      `Running ${chalk.cyan(
        '$ graphql create',
      )} ...                             `,
    )

    const args: any[] = ['create']

    if (this.args && this.args.dirName) {
      args.push(this.args!.dirName!)
    }

    if (boilerplate) {
      args.push('--boilerplate')
      args.push(boilerplate)
    }

    debug('running graphql cli')
    let binPath = path.join(
      __dirname,
      '../../../node_modules/graphql-cli/dist/bin.js',
    )
    if (!fs.pathExistsSync(binPath)) {
      binPath = path.join(__dirname, '../../../../graphql-cli/dist/bin.js')
    }

    if (!fs.pathExistsSync(binPath)) {
      binPath = 'graphql'
    }

    debug({ binPath, args })

    const result = spawn.sync(binPath, args, {
      stdio: 'inherit',
    })

    if (result.error) {
      if (result.error.message.includes('ENOENT')) {
        throw new Error(
          `Could not start graphql cli. Please try to install it globally with ${chalk.bold(
            'npm install -g graphql-cli',
          )}`,
        )
      }

      throw result.error
    }
  }
}
