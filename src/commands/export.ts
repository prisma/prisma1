import { downloadUrlMessage, exportingDataMessage } from '../utils/constants'
import out from '../io/Out'
import client from '../io/Client'
import { generateErrorOutput, parseErrors } from '../utils/errors'

export interface ExportProps {
  projectId: string
}

export interface ExportCliProps {
  env?: string
  project?: string
}

export default async({projectId}: ExportProps): Promise<void> => {
  try {
    out.startSpinner(exportingDataMessage)
    const url = await client.exportProjectData(projectId)
    const message = downloadUrlMessage(url)
    out.stopSpinner()
    out.write(message)
  } catch(e) {
    out.stopSpinner()
    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }
  }
}
