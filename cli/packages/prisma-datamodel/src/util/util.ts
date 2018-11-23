import * as pluralize from 'pluralize'
import { English } from './inflector/english'

export function capitalize(name: string) : string {
    return name.substring(0, 1).toUpperCase() + name.substring(1)
}

export function camelCase(name: string) : string {
    return name.substring(0, 1).toLowerCase() + name.substring(1)
}

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