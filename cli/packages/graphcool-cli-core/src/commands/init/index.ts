import { Command, flags, Flags, ProjectDefinition } from 'graphcool-cli-engine'
import * as chalk from 'chalk'
import { defaultDefinition, examples } from '../../examples'
import * as fs from 'fs-extra'
import * as path from 'path'
import * as inquirer from 'inquirer'
import {repeat, flatten} from 'lodash'

export default class Init extends Command {
  static topic = 'init'
  static description = 'Create a new project definition and target from scratch or based on an existing Graphcool definition.'
  static help = `
  
  ${chalk.green.bold('Examples:')}
  
  ${chalk.gray('-')} Initialize a new Graphcool project
    ${chalk.green('$ graphcool init')}
  `
  static flags: Flags = {
    copy: flags.string({
      char: 'c',
      description: 'ID or alias of the project, that the schema should be copied from',
    }),
    template: flags.string({
      char: 't',
      description:
        'The template to base the init on. (options: blank, instagram)',
    }),
  }

  static args = [
    {
      name: 'dirName',
      description: 'Folder to initialize in (optional)',
    },
  ]

  async run() {
    const { copy, template } = this.flags

    const dirName = this.args!.dirName

    if (dirName) {
      const newDefinitionPath = path.join(process.cwd(), dirName + '/')
      fs.mkdirpSync(newDefinitionPath)
      this.config.definitionDir = newDefinitionPath
      this.config.localRCPath = path.join(newDefinitionPath, '.graphcoolrc')
    }

    const files = fs.readdirSync(this.config.definitionDir)
    // the .graphcoolrc must be allowed for the docker version to be functioning
    // CONTINUE: special env handling for dockaa. can't just override the host/dinges
    if (files.length > 0 && !(files.length === 1 && files[0] === '.graphcoolrc')) {
      this.out.log(`
The directory ${chalk.green(this.config.definitionDir)} contains files that could conflict:

${files.map(f => `  ${f}`).join('\n')}

Either try using a new directory name, or remove the files listed above.

${chalk.bold(
        'NOTE:',
      )} The behavior of the init command changed, to deploy a project, please use ${chalk.green(
        'graphcool deploy',
      )}
Read more here: https://github.com/graphcool/graphcool/issues/706
`)
      this.out.exit(1)
    }

    if (template) {
      const projectDefinition = examples[template]
      if (!projectDefinition) {
        this.out.error(`${template} is not a valid template`)
      }
      this.definition.set(projectDefinition)
    }

    if (copy) {
      const info = await this.client.fetchProjectInfo(copy)
      this.definition.set(info.projectDefinition)
    }

    if (!this.definition.definition) {
      const newDefinition = await this.interactiveInit()
      this.definition.set(newDefinition)
    }

    this.out.log('\n')
    this.out.action.start(`Creating a new Graphcool app in ${chalk.green(this.config.definitionDir)}`)
    this.definition.save(undefined, false)
    this.out.action.stop()

    this.out.log(`${chalk.blue.bold('\nWritten files' + ':')}`)
    const createdFiles = flatten(this.definition.definition!.modules.map(module => Object.keys(module.files))).concat('graphcool.yml')
    this.out.filesTree(createdFiles)

    this.out.log(`\
Inside that directory, you can run the following commands:

  ${chalk.green('graphcool deploy')}
    Deploys the project to Graphcool

  ${chalk.green('graphcool deploy --target prod')}
    Deploys the project to Graphcool, using the target name \`prod\`

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
}
