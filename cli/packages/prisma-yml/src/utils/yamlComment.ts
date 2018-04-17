import * as yamlParser from 'yaml-ast-parser'
/**
 * Comments out the current entry of a specific key in a yaml document and creates a new value next to it
 * @param key key in yaml document to comment out
 * @param newValue new value to add in the document
 */
export function replaceYamlValue(input, key, newValue) {
  const ast = yamlParser.safeLoad(input)
  const position = getPosition(ast, key)
  const newEntry = `${key}: ${newValue}`
  if (!position) {
    return input + '\n' + newEntry
  }

  return (
    input.slice(0, position.start) +
    '#' +
    input.slice(position.start, position.end) +
    newEntry +
    input.slice(position.end)
  )
}

function getPosition(
  ast,
  key: string,
): { start: number; end: number } | undefined {
  const mapping = ast.mappings.find(m => m.key.value === key)
  if (!mapping) {
    return undefined
  }
  return {
    start: mapping.startPosition,
    end: mapping.endPosition + 1,
  }
}
