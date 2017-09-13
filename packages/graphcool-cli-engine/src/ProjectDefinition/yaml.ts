import * as Ajv from 'ajv'
import * as anyjson from 'any-json'
import schema = require('graphcool-json-schema/dist/schema.json')
import * as chalk from 'chalk'
import { GraphcoolDefinition } from 'graphcool-json-schema'
import { Output } from '../Output/index'
import Variables from './Variables'
const debug = require('debug')('yaml')

const ajv = new Ajv()

try {
  ajv.addMetaSchema(require('ajv/lib/refs/json-schema-draft-04.json'))
} catch (e) {
  // noop
}
const validate = ajv.compile(schema)

const __cache = {}

export async function readDefinition(
  file: string,
  out: Output,
): Promise<GraphcoolDefinition> {
  if (__cache[file]) {
    debug(`Getting definition from cache`)
    return __cache[file]
  }
  const json = await anyjson.decode(file, 'yaml') as GraphcoolDefinition
  const valid = validate(json)
  // TODO activate as soon as the backend sends valid yaml
  if (!valid) {
    out.log(chalk.bold('Errors while validating graphcool.yml:\n'))
    out.error(
      chalk.red(
        ajv
          .errorsText(validate.errors)
          .split(', ')
          .map(l => `  ${l}`)
          .join('\n'),
      ),
    )
    out.exit(1)
  }

  const vars = new Variables(json, out)
  const populatedJson = await vars.populateDefinition(json)
  if (populatedJson.custom) {
    delete populatedJson.custom
  }

  __cache[file] = populatedJson
  return populatedJson
}
