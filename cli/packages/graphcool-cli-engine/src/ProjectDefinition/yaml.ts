import * as Ajv from 'ajv'
import * as yaml from 'js-yaml'
import schema = require('graphcool-json-schema/dist/schema.json')
import chalk from 'chalk'
import { GraphcoolDefinition } from 'graphcool-json-schema'
import { Output } from '../Output/index'
import Variables from './Variables'
import { Args } from '../types/common'
const debug = require('debug')('yaml')

const ajv = new Ajv()

try {
  ajv.addMetaSchema(require('ajv/lib/refs/json-schema-draft-04.json'))
} catch (e) {
  // noop
}
const validate = ajv.compile(schema)

const cache = {}

export async function readDefinition(
  file: string,
  out: Output,
  moduleName: string,
  args: Args
): Promise<GraphcoolDefinition> {
  wildcardCheck(file, out)
  const json = yaml.safeLoad(file) as GraphcoolDefinition

  const vars = new Variables(out, moduleName, args)
  const populatedJson = await vars.populateJson(json)
  if (populatedJson.custom) {
    delete populatedJson.custom
  }
  const valid = validate(populatedJson)
  // TODO activate as soon as the backend sends valid yaml
  if (!valid) {
    out.log(
      out.getErrorPrefix(moduleName) +
        chalk.bold('Errors while validating graphcool.yml:\n'),
    )
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

  cache[file] = populatedJson
  return populatedJson
}

function wildcardCheck(file: string, out: Output) {
  const regex = /.*?operation:\s*\*/m

  const match = file.match(regex)
  if (match) {
    out.error(`To use the wildcard permission, please wrap the asterisk with two double quotes:
${chalk.green('- operation: "*"')}`)
  }
}
