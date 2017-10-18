import * as path from 'path'
import chalk from 'chalk'
import fs from '../fs'
import { Output } from '../Output/index'
import { GraphcoolModule, ProjectDefinition } from '../types/common'
require('source-map-support/register')

export function projectToFs(project: ProjectDefinition, outputDir: string, out: Output, files?: string[], silent?: boolean): void {
  for (const module of project.modules) {
    moduleToFs(module, outputDir, out, files, silent)
  }
}

function moduleToFs(module: GraphcoolModule, outputDir: string, out: Output, files?: string[], silent?: boolean) {
  if ((files && files.includes('graphcool.yml') || !files)) {
    const ymlPath = path.join(outputDir, 'graphcool.yml')
    fs.writeFileSync(ymlPath, module.content)
    // if (!silent) {
    //   out.log(chalk.blue(`Written to graphcool.yml\n`))
    // }
  }

  const fileNames = files ? Object.keys(module.files).filter(f => files.includes(f)) : Object.keys(module.files)

  for (const relativePath of fileNames) {
    const content = module.files[relativePath]
    const filePath = path.join(outputDir, relativePath)
    const dir = path.dirname(filePath)

    let currentFile: null | string = null

    try {
      currentFile = fs.readFileSync(filePath, 'utf-8')
    } catch (e) {
      // ignore if file doesn't exist yet
    }

    fs.mkdirpSync(dir)
    fs.writeFileSync(filePath, content)
    // if (!silent) {
    //   out.log(chalk.blue(`Written to ${relativePath}\n`))
    // }
  }
}
