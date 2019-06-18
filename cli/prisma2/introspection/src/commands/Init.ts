import { Command, Env, arg, format } from '@prisma/cli'
import { LiftEngine } from '@prisma/lift'
import { isError } from 'util'
import { promptInteractively } from '../prompt'
import { introspect } from '../introspect/util'
import chalk from 'chalk'
import figures = require('figures')
import { writeFileSync, mkdirSync, existsSync } from 'fs'
import { join } from 'path'
import { findTemplate } from '../templates'
import { loadStarter } from '../loader'
import { defaultPrismaConfig } from '../defaults'
import { mkdirpSync } from 'fs-extra'
import { InitPromptResult } from '../types'

export class Init implements Command {
  lift: LiftEngine
  static new(env: Env): Init {
    return new Init(env)
  }

  private constructor(private readonly env: Env) {
    this.lift = new LiftEngine({
      projectDir: env.cwd,
    })
  }

  async parse(argv: string[]): Promise<any> {
    // parse the arguments according to the spec
    const args = arg(argv, {})

    if (isError(args)) {
      return null
    }

    if (args['--help']) {
      return this.help()
    }

    const outputDirName = args._[0]
    const outputDir = outputDirName ? join(this.env.cwd, outputDirName) : this.env.cwd

    if (existsSync(join(outputDir, 'datamodel.prisma'))) {
      throw new Error(`Can't start ${chalk.bold('prisma init')} as ${chalk.redBright(
        join(outputDir, 'datamodel.prisma'),
      )} exists.
Please run ${chalk.bold('prisma init')} in an empty directory.`)
    }

    if (existsSync(join(outputDir, 'project.prisma'))) {
      throw new Error(`Can't start ${chalk.bold('prisma init')} as ${chalk.redBright(
        join(outputDir, 'project.prisma'),
      )} exists.
Please run ${chalk.bold('prisma init')} in an empty directory.`)
    }

    if (outputDirName) {
      try {
        // Create the output directories if needed (mkdir -p)
        mkdirSync(outputDir, { recursive: true })
      } catch (e) {
        if (e.code !== 'EEXIST') throw e
      }
    }

    try {
      const result = await promptInteractively(introspect, 'init')

      const template = findTemplate(result.initConfiguration.template, result.initConfiguration.language)
      if (!template) {
        mkdirpSync(join(outputDir, 'prisma'))
        writeFileSync(join(outputDir, 'prisma/project.prisma'), defaultPrismaConfig(result))
        console.log(`Your template has been successfully set up!
  
Here are the next steps to get you started:
  1. Run ${chalk.yellow(`yarn global add prisma2`)} to install the Prisma 2 CLI. 
  2. Run ${chalk.yellow(`prisma2 lift save --name 'init'`)} to create a migration locally. 
  3. Run ${chalk.yellow(`prisma2 lift up`)} to apply the migrations to your local db. 
  4. That's it !`)
        return
      }

      await loadStarter(template, process.cwd(), {
        installDependencies: true,
      })

      if (result.introspectionResult && result.introspectionResult.sdl) {
        writeFileSync(join(outputDir, 'prisma/project.prisma'), result.introspectionResult.sdl)
      }

      this.patchPrismaConfig(result, outputDir)

      this.printFinishMessage()
    } catch (e) {
      console.error(e)
    }

    process.exit(0)
  }

  patchPrismaConfig(result: InitPromptResult, outputDir: string) {
    if (!result.introspectionResult) {
      return
    }
    mkdirpSync(join(outputDir, 'prisma'))
    writeFileSync(join(outputDir, 'prisma/project.prisma'), result.introspectionResult.sdl)
  }

  printFinishMessage() {
    console.log(
      format(`
${chalk.green(`${figures.tick} Your all set!`)}

  ${'─'.repeat(50)}

  ${chalk.bold('We created the following files for you:')}

  ${chalk.bold('project.prisma')}    ${chalk.dim('The datamodel describes your database schema')}
  ${chalk.bold('main.ts')}             ${chalk.dim('A simple script to get you started')}
  ${chalk.bold('tsconfig.json')}       ${chalk.dim('TypeScript configuration')}
  ${chalk.bold('node_modules')}       ${chalk.dim('Required Node dependencies (includes Photon)')}

  ${'─'.repeat(50)}

  ${chalk.bold('Run the following commands to start developing')}

  $ cd hello-world
  $ prisma dev

  Learn more about using Photon and Lift at
  https://www.prisma.io/docs/...
    `),
    )
  }

  help() {
    return console.log(
      format(`
Usage: prisma init

Initialize files for a new Prisma project
    `),
    )
  }
}

// async function run() {
//   const env = await Env.load(process.env, process.cwd())
//   if (isError(env)) {
//     console.error(env)
//     return 1
//   }
//   // create a new CLI with our subcommands
//   const cli = Init.new(env)
//   // parse the arguments
//   const result = await cli.parse(process.argv.slice(2))
// }

// run()
