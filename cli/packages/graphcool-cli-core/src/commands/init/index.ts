import { Command, flags, Flags, ProjectDefinition } from 'graphcool-cli-engine'
import chalk from 'chalk'
import { defaultDefinition, defaultPjson, examples } from '../../examples'
import * as fs from 'fs-extra'
import * as path from 'path'
import * as inquirer from 'inquirer'
import {repeat, flatten} from 'lodash'

export default class Init extends Command {
  static topic = 'init'
  static description = 'Create files for new services'
  static group = 'general'
  static help = `
  
  ${chalk.green.bold('Examples:')}
  
  ${chalk.gray('-')} Initialize a new Graphcool project
    ${chalk.green('$ graphcool init')}
  `
  static flags: Flags = {
    force: flags.boolean({
      char: 'f',
      description: 'Initialize even if the folder already contains graphcool files',
    }),
    copy: flags.string({
      char: 'c',
      description: 'ID or alias of the project, that the schema should be copied from',
    }),
  }

  static args = [
    {
      name: 'dirName',
      description: 'Folder to initialize in (optional)',
    },
  ]

  async run() {
    const { copy, force } = this.flags

    const dirName = this.args!.dirName

    if (dirName) {
      const newDefinitionPath = path.join(process.cwd(), dirName + '/')
      fs.mkdirpSync(newDefinitionPath)
      this.config.definitionDir = newDefinitionPath
      this.config.localRCPath = path.join(newDefinitionPath, '.graphcoolrc')
    }

    const pjson = {
      ...defaultPjson,
      name: path.basename(this.config.definitionDir)
    }

    const files = fs.readdirSync(this.config.definitionDir)
    // the .graphcoolrc must be allowed for the docker version to be functioning
    // CONTINUE: special env handling for dockaa. can't just override the host/dinges
    if (files.length > 0 && !(files.length === 1 && files[0] === '.graphcoolrc') && files.includes('graphcool.yml')) {
      this.out.log(`
The directory ${chalk.green(this.config.definitionDir)} contains files that could conflict:

${files.map(f => `  ${f}`).join('\n')}

Either try using a new directory name, or remove the files listed above.

${chalk.bold(
        'NOTE:',
      )} The behavior of the init command changed, to deploy a project, please use ${chalk.green(
        'graphcool deploy',
      )}

To force the init process in this folder, use ${chalk.green('graphcool init --force')}`)
      if (force) {
        await this.askForConfirmation(this.config.definitionDir)
      } else {
        this.out.exit(1)
      }
    }
    //
    // if (template) {
    //   const projectDefinition = examples[template]
    //   if (!projectDefinition) {
    //     this.out.error(`${template} is not a valid template`)
    //   }
    //   this.definition.set(projectDefinition)
    // }

    if (copy) {
      const info = await this.client.fetchProjectInfo(copy)
      this.definition.set(info.projectDefinition)
    }

    if (!this.definition.definition) {
      // const newDefinition = await this.interactiveInit()
      const newDefinition = defaultDefinition
      this.definition.set(newDefinition)
    }

    let relativeDir = path.relative(process.cwd(), this.config.definitionDir)
    relativeDir = relativeDir.length === 0 ? '.' : relativeDir
    this.out.action.start(`Creating a new Graphcool service in ${chalk.green(relativeDir)}`)
    this.definition.save(undefined, false)
    this.out.action.stop()

    this.out.log(`${chalk.dim.bold('\nWritten files' + ':')}`)
    fs.writeFileSync(path.join(this.config.definitionDir, 'package.json'), JSON.stringify(pjson, null, 2))
    const createdFiles = flatten(this.definition.definition!.modules.map(module => Object.keys(module.files))).concat(['graphcool.yml', 'package.json'])
    this.out.filesTree(createdFiles)

    const cdInstruction = relativeDir === '.' ? '' : `To get started, cd into the new directory:
  ${chalk.green(`cd ${relativeDir}`)}
`

    this.out.log(`${cdInstruction}
To deploy your Graphcool service:
  ${chalk.green('graphcool deploy')}

To start your local Graphcool cluster:
  ${chalk.green('graphcool local up')}
  
To add facebook authentication to your service:
  ${chalk.green('graphcool add-template facebook-auth')}

You can find further instructions in the ${chalk.green('graphcool.yml')} file,
which is the central project configuration.
`)
  }

  async interactiveInit(): Promise<ProjectDefinition> {
    const initQuestion = {
      name: 'init',
      type: 'list',
      message: 'How do you want to start?',
      choices: [
        {
          value: 'blank',
          name: [
            `${chalk.bold('New blank project')}`,
            `  Creates a new Graphcool project from scratch.`,
            '',
          ].join('\n'),
        },
        {
          value: 'copy',
          name: [
            `${chalk.bold('Copying an existing project')}`,
            `  Copies a project from your account`,
            '',
          ].join('\n'),
        },
        // {
        //   value: 'example',
        //   name: [
        //     `${chalk.bold('Based on example')}`,
        //     `  Creates a new Graphcool project based on an example`,
        //     '',
        //   ].join('\n'),
        // },
      ],
      pageSize: 13,
    }

    const { init } = await this.out.prompt([initQuestion])

    switch (init) {
      case 'blank':
        this.out.up(7)
        return defaultDefinition
      case 'copy':
        await this.auth.ensureAuth()
        const projectId = await this.projectSelection()
        this.out.up(4)
        const info = await this.client.fetchProjectInfo(projectId)
        return info.projectDefinition
      case 'example':
        return await this.exampleSelection()
    }

    return null as any
  }

  private async projectSelection() {
    const projects = await this.client.fetchProjects()
    const choices = projects.map(p => ({
      name: `${p.name} (${p.id})`,
      value: p.id,
    })).concat(new inquirer.Separator(chalk.bold.green(repeat('-', 50))))

    const question = {
      name: 'project',
      type: 'list',
      message: 'Please choose a project',
      choices,
      pageSize: Math.min(process.stdout.rows!, projects.length) - 2,
    }

    const { project } = await this.out.prompt([question])

    return project
  }

  private async exampleSelection(): Promise<ProjectDefinition> {
    const question = {
      name: 'example',
      type: 'list',
      message: 'Please choose an example',
      choices: [
        {
          value: 'instagram',
          name: [
            `${chalk.bold('Instagram')}`,
            `Contains an instagram clone with permission logic`,
            '',
          ].join('\n'),
        },
        {
          value: 'stripe',
          name: [
            `${chalk.bold('Stripe Checkout')}`,
            `An example integrating the stripe checkout with schema extensions`,
            '',
          ].join('\n'),
        },
        {
          value: 'sendgrid',
          name: [
            `${chalk.bold('Sendgrid Mails')}`,
            `An example that shows how to connect Graphcool to the Sendgrid API`,
            '',
          ].join('\n'),
        },
      ],
      pageSize: 12,
    }

    const { example } = await this.out.prompt(question)

    return examples[example]
  }

  private async askForConfirmation(folder: string) {
    const confirmationQuestion = {
      name: 'confirmation',
      type: 'input',
      message: `Are you sure that you want to init a new service in ${chalk.green(folder)}? y/N`,
      default: 'n'
    }
    const {confirmation}: {confirmation: string} = await this.out.prompt(confirmationQuestion)
    if (confirmation.toLowerCase().startsWith('n')) {
      this.out.exit(0)
    }
  }
}
