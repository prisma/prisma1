import {SystemEnvironment} from '../types'
import { deleteProject, parseErrors, generateErrorOutput } from '../api/api'
import {
  deletingProjectMessage,
  deletedProjectMessage
} from '../utils/constants'

interface Props {
  sourceProjectId?: string
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  if (props.sourceProjectId) {
    out.startSpinner(deletingProjectMessage(props.sourceProjectId))

    try {
      const id = await deleteProject(props.sourceProjectId, resolver)
      out.stopSpinner()
      out.write(deletedProjectMessage)

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

}