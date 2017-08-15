import * as path from 'path'
import * as fs from 'fs'
import { GraphcoolModule, ProjectDefinition } from '../types'
import * as mkdirp from 'mkdirp'

export default async function projectToFs(project: ProjectDefinition, outputDir: string): Promise<any> {
  for (const module of project.modules) {
    await moduleToFs(module, outputDir)
  }
}

async function moduleToFs(module: GraphcoolModule, outputDir: string) {
  fs.writeFileSync(path.join(outputDir, 'project.gcl'), module.content)

  for (const relativePath in module.files) {
    const content = module.files[relativePath]
    const filePath = path.join(outputDir, relativePath)
    const dir = path.dirname(filePath)

    mkdirp.sync(dir)
    fs.writeFileSync(filePath, content)
  }
}
