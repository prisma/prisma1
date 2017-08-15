import * as path from 'path'
import * as fs from 'fs'
import { ProjectDefinition, GraphcoolModule } from '../types'
import gclToJson from 'gcl-lib'
import { GCL } from '../gcl-types'

export default async function fsToProject(inputDir: string): Promise<ProjectDefinition> {

  const schema = fs.readFileSync(path.join(__dirname, '../../../example/gcl-schema.graphql'), 'utf-8')
  const definition = fs.readFileSync(path.join(inputDir, 'project.gcl'), 'utf-8')

  let module: GraphcoolModule = {
    name: '',
    content: definition,
    files: {}
  }

  let files = {}

  const gcl: GCL = await gclToJson(definition, schema)

  const databaseSchema = fs.readFileSync(path.join(inputDir, gcl.databaseSchema.src), 'utf-8')
  files = {
    ...files,
    [gcl.databaseSchema.src]: databaseSchema,
  }

  gcl.modelPermissions.forEach(modelPermission => {
    if (modelPermission.query) {
      const permissionQuery = fs.readFileSync(path.join(inputDir, modelPermission.query.src), 'utf-8')
      files = {
        ...files,
        [modelPermission.query.src]: permissionQuery,
      }
    }
  })

  gcl.relationPermissions.forEach(relationPermission => {
    if (relationPermission.query) {
      const permissionQuery = fs.readFileSync(path.join(inputDir, relationPermission.query.src), 'utf-8')
      files = {
        ...files,
        [relationPermission.query.src]: permissionQuery,
      }
    }
  })

  gcl.functions.forEach(func => {
    if (func.handler.code) {
      const functionCode = fs.readFileSync(path.join(inputDir, func.handler.code.src), 'utf-8')
      files = {
        ...files,
        [func.handler.code.src]: functionCode,
      }
    }

    if (func.serversideSubscription) {
      if (func.serversideSubscription.subscriptionQuery) {
        const file = fs.readFileSync(path.join(inputDir, func.serversideSubscription.subscriptionQuery.src), 'utf-8')
        files = {
          ...files,
          [func.serversideSubscription.subscriptionQuery.src]: file,
        }
      }

      if (func.serversideSubscription.schemaExtension) {
        const file = fs.readFileSync(path.join(inputDir, func.serversideSubscription.schemaExtension.src), 'utf-8')
        files = {
          ...files,
          [func.serversideSubscription.schemaExtension.src]: file,
        }
      }
    }

  })

  return {
    modules: [{
      ...module,
      files,
    }]
  }
}
