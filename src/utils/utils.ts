import {ProjectInfo} from '../types'

export function projectInfoToContents(projectInfo: ProjectInfo): string {
  return `# project: ${projectInfo.alias || projectInfo.projectId}\n# version: ${projectInfo.version}\n\n${projectInfo.schema}`
}
