import { Command, flags, Flags } from 'graphcool-cli-engine'
import * as fs from 'fs-extra'
import * as path from 'path'
import chalk from 'chalk'

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

    const files = fs.readdirSync(this.config.definitionDir)
    // the .graphcoolrc must be allowed for the docker version to be functioning
    // CONTINUE: special env handling for dockaa. can't just override the host/dinges
    if (
      files.length > 0 &&
      (files.includes('graphcool.yml') || files.includes('datamodel.graphql'))
    ) {
      this.out.log(`
The directory ${chalk.green(
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
    const newGraphcoolYml = graphcoolYml.replace(
      'SERVICE_NAME',
      path.basename(this.config.definitionDir),
    )
    fs.writeFileSync(definitionPath, newGraphcoolYml)

    const cdInstruction =
      relativeDir === '.'
        ? ''
        : `To get started, cd into the new directory:
  ${chalk.green(`cd ${relativeDir}`)}
`

    this.out.log(`${cdInstruction}
To deploy your Graphcool service:
  ${chalk.green('graphcool deploy')}

To start your local Graphcool cluster:
  ${chalk.green('graphcool local up')}

You can find further instructions in the ${chalk.green('graphcool.yml')} file,
which is the central service configuration.
`)
  }
}
