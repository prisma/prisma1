import { getGraphQLConfig } from 'graphql-config'
import { values } from 'lodash'

export function getSchemaPathFromConfig(rootDir?: string): string | null {
  try {
    const config = getGraphQLConfig(rootDir).config
    if (config) {
      // first look on top-level
      if (config.extensions && config.extensions.prisma && config.schemaPath) {
        return config.schemaPath
      }

      const prismaProject = values(config.projects).find(
        p => p.extensions && p.extensions.prisma,
      )
      if (prismaProject && prismaProject.schemaPath) {
        return prismaProject.schemaPath
      }
    }
  } catch (e) {
    //
  }

  return null
}
