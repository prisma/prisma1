import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'
import * as npmRun from 'npm-run'

export default class Init extends Command {
  static topic = 'init'
  static description = 'Initialize a new service'

  static args = [
    {
      name: 'dirName',
      description: 'Folder to initialize in (optional)',
    },
  ]

  async run() {
    const dirName = this.args!.dirName
    if (dirName) {
      const newDefinitionDir = path.join(process.cwd(), dirName + '/')
      this.config.definitionDir = newDefinitionDir
      fs.mkdirpSync(newDefinitionDir)
    } else {
      this.config.definitionDir = process.cwd()
    }

    const choice = await this.prompt()

    if (choice === 'create') {
      await this.graphqlCreate()
    } else {
      this.initMinimal()
    }
  }

  async prompt(): Promise<'create' | 'minimal'> {
    const question = {
      name: 'prompt',
      type: 'list',
      message: `How to set up a new Graphcool service?`,
      choices: [
        {
          name: 'GraphQL server/fullstack boilerplate (recommended)',
          value: 'create',
        },
        {
          name: 'Minimal setup: database-only',
          value: 'minimal',
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
    // the .graphcoolrc must be allowed for the docker version to be functioning
    // CONTINUE: special env handling for dockaa. can't just override the host/dinges
    if (
      files.length > 0 &&
      (files.includes('graphcool.yml') || files.includes('datamodel.graphql'))
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

    const definitionPath = path.join(this.config.definitionDir, 'graphcool.yml')
    const graphcoolYml = fs.readFileSync(definitionPath, 'utf-8')

    this.out.log(`\
Created 3 new files:               

  ${chalk.cyan('graphcool.yml')}        Graphcool service definition
  ${chalk.cyan(
    'datamodel.graphql',
  )}    GraphQL SDL-based datamodel (foundation for database)
  ${chalk.cyan(
    '.graphqlconfig.yml',
  )}   Configuration file for GraphQL tooling (Playground, IDE, …)

You can now run ${chalk.cyan(
      '$ graphcool deploy',
    )} to deploy your database service.

For next steps follow this tutorial: https://bit.ly/graphcool-first-steps`)
  }
  async graphqlCreate() {
    this.out.log(
      `Running ${chalk.cyan(
        '$ graphql create',
      )} ...                             `,
    )

    const args: any[] = ['create']

    if (this.args && this.args.dirName) {
      args.push(this.args!.dirName!)
    }

    npmRun.spawnSync('graphql', args, {
      stdio: 'inherit',
    })
  }
}
