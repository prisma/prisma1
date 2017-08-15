import * as path from 'path'
import * as fs from 'fs'
import { GraphcoolModule, ProjectDefinition } from '../types'
import * as mkdirp from 'mkdirp'
import * as concordance from 'concordance'
import concordanceOptions from './concordance-options'

export default async function projectToFs(project: ProjectDefinition, outputDir: string): Promise<any> {
  for (const module of project.modules) {
    await moduleToFs(module, outputDir)
  }
}

function formatDescriptorDiff(actualDescriptor, expectedDescriptor, options) {
  options = Object.assign({}, options, concordanceOptions);
  return {
    label: 'Difference:',
    formatted: concordance.diffDescriptors(actualDescriptor, expectedDescriptor, options)
  };
}

async function moduleToFs(module: GraphcoolModule, outputDir: string, force: boolean = false) {
  fs.writeFileSync(path.join(outputDir, 'project.gcl'), module.content)

  for (const relativePath in module.files) {
    const content = module.files[relativePath]
    const filePath = path.join(outputDir, relativePath)
    const dir = path.dirname(filePath)

    let currentFile: null | string = null

    try {
      currentFile = fs.readFileSync(filePath, 'utf-8')
    } catch (e) {
      // ignore if file doesn't exist yet
    }

    if (currentFile !== null && currentFile !== content) {
      const localDescriptor = concordance.describe(currentFile, concordanceOptions)
      const remoteDescriptor = concordance.describe(content, concordanceOptions)
      const diff = concordance.diffDescriptors(localDescriptor, remoteDescriptor)
      console.log(diff)
      throw new Error(`The remote version of ${relativePath} as changed. Use --force to override it.`)
    }

    mkdirp.sync(dir)
    fs.writeFileSync(filePath, content)
  }
}
