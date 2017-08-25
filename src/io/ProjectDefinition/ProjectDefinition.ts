// long name to avoid naming conflict with the ts type definition
import { ProjectDefinition } from '../../types'
import fsToProject from './fsToProject'
import projectToFs from './projectToFs'

class ProjectDefinitionClass {
  definition: ProjectDefinition

  public async load() {
    this.definition = await fsToProject(process.cwd())
  }

  public async save() {
    await projectToFs(this.definition, process.cwd())
  }
}


const definition = new ProjectDefinitionClass()

export default definition
