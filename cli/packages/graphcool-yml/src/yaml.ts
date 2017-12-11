import * as Ajv from 'ajv'
import * as yaml from 'js-yaml'
import * as fs from 'fs-extra'
import schema = require('graphcool-json-schema/dist/schema.json')
import { GraphcoolDefinition } from 'graphcool-json-schema'
import { Variables } from './Variables'
import { Args } from './types/common'
import { Output, IOutput } from './Output'
const debug = require('debug')('yaml')

const ajv = new Ajv()

const validate = ajv.compile(schema)

const cache = {}

export async function readDefinition(
  filePath: string,
  args: Args,
  out: IOutput = new Output(),
): Promise<GraphcoolDefinition> {
  if (!fs.pathExistsSync(filePath)) {
    throw new Error(`${filePath} could not be found.`)
  }
  const file = fs.readFileSync(filePath, 'utf-8')
  const json = yaml.safeLoad(file) as GraphcoolDefinition

  const vars = new Variables(filePath, args, out)
  const populatedJson = await vars.populateJson(json)
  if (populatedJson.custom) {
    delete populatedJson.custom
  }
  const valid = validate(populatedJson)
  // TODO activate as soon as the backend sends valid yaml
  if (!valid) {
    let errorMessage =
      out.getErrorPrefix(filePath) +
      'Errors while validating graphcool.yml:\n' +
      ajv
        .errorsText(validate.errors!)
        .split(', ')
        .map(l => `  ${l}`)
        .join('\n')
    throw new Error(errorMessage)
  }

  cache[file] = populatedJson
  return populatedJson
}
