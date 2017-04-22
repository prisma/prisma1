import {Region, Resolver, SchemaInfo} from '../types'
import {
  graphcoolProjectFileName,
  noProjectFileMessage
} from '../utils/constants'
import * as fs from 'fs'
import {readProjectIdFromProjectFile, readDataModelFromProjectFile} from '../utils/file'
import {pushNewSchema} from '../api/api'


interface Props {
  isDryRun?: boolean
}

export default async(props: Props, resolver: Resolver): Promise<void> => {
  if (!fs.existsSync(graphcoolProjectFileName)) {
    process.stdout.write(noProjectFileMessage)
    process.exit(1)
  }

  const projectId = readProjectIdFromProjectFile(resolver)
  const newSchema = readDataModelFromProjectFile(resolver)
  const isDryRun = props.isDryRun || true

  // TODO: check against remote schema to see if there are any changes

  try {

    const migrationResult = pushNewSchema(projectId, newSchema, isDryRun, resolver)
    

  }



}