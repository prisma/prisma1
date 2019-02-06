import * as pluralize from 'pluralize'
import { English } from './inflector/english'

/**
 * Converts the first character of a word to upper case
 * @param name 
 */
export function capitalize(name: string) : string {
    return name.substring(0, 1).toUpperCase() + name.substring(1)
}

/**
 * Converts the first character of a word to lower case. 
 * @param name 
 */
export function camelCase(name: string) : string {
    return name.substring(0, 1).toLowerCase() + name.substring(1)
}

/**
 * Pluralizes a word like the prisma server would do.
 * @param name The word to pluralize.
 */
export function plural(name: string) : string {
    const pluralWord = English.plural(name)
    if(pluralWord === null || pluralWord === name) {
        if(name.endsWith('s')) {
            return name + 'es'
        } else {
            return name + 's'
        }
    } else {
        return pluralWord
    }
}

/**
 * Removes all indent from a multi-line string literal.
 * The indent is the smalles indent off all non-empty lines.
 * If the first line is empty, it is removed.
 * @param literal 
 */
export function dedent(literal: string) : string {
    const lines = literal.split('\n')
    if(lines.length === 0) {
        return ''
    }
    if(lines[0].length === 0) {
        lines.splice(0, 1)
    }

    // Find minimum indent
    let indent = -1
    for(const line of lines) {
        const trimmed = line.trimLeft()
        if(trimmed.length === 0) {
            continue
        }
        const lineIndent = line.indexOf(trimmed)
        
        if(indent === -1) {
            indent = lineIndent
        } else {
            indent = Math.min(lineIndent, indent)
        }
    }

    // Dedent document
    for(let i = 0; i < lines.length; i++) {
        if(lines[i].length < indent) {
            lines[i] = '' // Line containing only whitespace or empty.
        } else {
            lines[i] = lines[i].substring(indent)
        }
    }

    return lines.join('\n')
}
