import * as Ajv from 'ajv'
import * as yaml from 'js-yaml'
import * as fs from 'fs-extra'
import schema = require('graphcool-json-schema/dist/schema.json')
import { GraphcoolDefinition } from 'graphcool-json-schema'
import { Variables } from './Variables'
import { Args } from './types/common'
import { Output, IOutput } from './Output'
const debug = require('debug')('yaml')
import * as stringify from 'json-stable-stringify'

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
    debugger
    let errorMessage =
      out.getErrorPrefix(filePath) + '\n' + printErrors(validate.errors!)
    throw new Error(errorMessage)
  }

  cache[file] = populatedJson
  return populatedJson
}

function printErrors(errors, name = 'graphcool.yml') {
  return errors
    .map(e => {
      const paramsKey = stringify(e.params)
      if (betterMessagesByParams[paramsKey]) {
        return betterMessagesByParams[paramsKey]
      }
      const params = Object.keys(e.params)
        .map(key => `${key}: ${e.params[key]}`)
        .join(', ')
      debug(stringify(e.params))
      return `${name}${e.dataPath} ${e.message}. ${params}`
    })
    .join('\n')
}

const betterMessagesByParams = {
  '{"additionalProperty":"stages"}':
    'graphcool.yml should NOT have a "stages" property anymore. Stages are now just provided as CLI args. Read more here: https://goo.gl/SUD5i5',
}
