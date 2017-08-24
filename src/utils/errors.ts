import {
  APIError,
} from '../types'

import { contactUsInSlackMessage } from './constants'

export function parseErrors(response: any): APIError[] {
  const errors: APIError[] = response.errors.map(error => ({
    message: error.message,
    requestId: error.requestId,
    code: String(error.code)
  }))

  return errors
}

export function generateErrorOutput(apiErrors: APIError[]): string {
  const lines = apiErrors.map(error => `${error.message} (Request ID: ${error.requestId})`)
  const output = `\n${lines.join('\n')}\n\n${contactUsInSlackMessage}`
  return output
}
