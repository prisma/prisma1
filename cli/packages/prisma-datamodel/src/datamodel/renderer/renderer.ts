import { ISDL, IGQLType, IDirectiveInfo, IGQLField } from "../model"
import { GraphQLSchema } from "graphql/type/schema"
import { GraphQLObjectType, GraphQLEnumType, GraphQLField, GraphQLFieldConfig } from "graphql/type/definition"
import { GraphQLDirective } from "graphql/type/directives"
import { DirectiveKeys } from "../directives";
import { TypeIdentifiers } from "../scalar";

const indent = '  '
const comment = '#'

export default class Renderer {
  public render(input: ISDL): string {

    // Sort alphabetically. Enums last. 
    const sortedTypes = [...input.types].sort(
      (a, b) => {
        if(a.isEnum === b.isEnum) {
          return a.name.toLowerCase() > b.name.toLowerCase() ? 1 : -1
        } else if(b.isEnum) {
          return -1
        } else {
          return 1
        }
      }
    )

    return sortedTypes.map(t => {
      if(t.isEnum) {
        return this.renderEnum(t)
      } else {
        return this.renderType(t)
      }
    }).join(`\n\n`)
  }

  protected renderType(type: IGQLType): string {
    const typedirectives: IDirectiveInfo[] = type.directives || []

    // TODO Move direction magic to superclass
    if(type.isEmbedded) { typedirectives.push({ name: DirectiveKeys.isEmbedded, arguments: {} }) }

    const renderedDirectives = this.renderDirectives(typedirectives)
    const sortedFields = [...type.fields].sort((a, b) => a.name.toLowerCase() > b.name.toLowerCase() ? 1 : -1)
    const renderedFields = sortedFields.map(x => this.renderField(x))

    const renderedTypeName = renderedDirectives.length > 0 ?
    `type ${type.name} ${renderedDirectives}` :
    `type ${type.name}`

    const { renderedComments, hasError } = this.renderComments(type, '')
    const allFieldsHaveError = type.fields.every(x => x.comments !== undefined && x.comments.some(c => c.isError))

    const commentPrefix = allFieldsHaveError ? `${comment} ` : ''


    if(renderedComments.length > 0) {
      return `${renderedComments}\n${commentPrefix}${renderedTypeName} {\n${renderedFields.join('\n')}\n${commentPrefix}}`
    } else {
      return `${commentPrefix}${renderedTypeName} {\n${renderedFields.join('\n')}\n${commentPrefix}}`
    }
  }

  protected renderComments(type: IGQLType | IGQLField, spacing: string) {
    const renderedComments = type.comments !== undefined ? type.comments.map(x => `${spacing}${comment} ${x.text}`).join('\n') : []
    const hasError =  type.comments !== undefined ? type.comments.some(x => x.isError) : false

    return { renderedComments, hasError }
  }

  protected renderField(field: IGQLField) : string {
    const fieldDirectives: IDirectiveInfo[] = field.directives || []

    // TODO Move direction magic to superclass
    if(field.defaultValue !== null) { fieldDirectives.push({ name: DirectiveKeys.default, arguments: { value: this.renderValue(field.type, field.defaultValue) }}) }
    if(field.isUnique) { fieldDirectives.push({ name: DirectiveKeys.isUnique, arguments: {} }) }
    if(field.relationName !== null) { fieldDirectives.push({ name: DirectiveKeys.relation, arguments: { name: field.relationName } }) }
    if(field.isId) { fieldDirectives.push({ name: DirectiveKeys.isId, arguments: { } }) } 

    const renderedDirectives = this.renderDirectives(fieldDirectives)
    
    let type = this.extractTypeIdentifier(field.type)
    if(field.isList) {
      // Lists are always required in Prisma
      type = `[${type}!]!`
    }
    else if(field.isRequired) {
      type = `${type}!`
    }

    const renderedField = renderedDirectives.length > 0 ?
      `${field.name}: ${type} ${renderedDirectives}` :
      `${field.name}: ${type}`

    const { renderedComments, hasError } = this.renderComments(field, indent)

    if(renderedComments.length > 0) {
      if(hasError) {
        return `${renderedComments}\n${indent}${comment} ${renderedField}`
      } else {
        return `${renderedComments}\n${indent}${renderedField}`
      }
    } else {
      return `${indent}${renderedField}`
    }
  }

  protected renderEnum(type: IGQLType): string {   
    const values: string[] = []

    for(const field of type.fields) {
      if(field.defaultValue !== null) {
        values.push(`${indent}${field.name} = ${this.renderValue(field.type as string, field.defaultValue)}`)
      } else {
        values.push(`${indent}${field.name}`)
      }
    }

    return `enum ${type.name} {\n${values.join('\n')}\n}`
  }

  protected renderDirectives(directives: IDirectiveInfo[]) : string {
    const sortedDirectives = [...directives].sort((a, b) => a.name.toLowerCase() > b.name.toLowerCase() ? 1 : -1)
    return this.mergeDirectives(sortedDirectives).map(x => this.renderDirective(x)).join(` `)
  }

  protected renderDirective(directive: IDirectiveInfo): string {
    const args: string[] = []

    for(const arg of Object.keys(directive.arguments)) {
      // TODO: We don't know the type. Is this a problem?
      args.push(`${arg}: ${this.renderValue('', directive.arguments[arg])}`)
    }

    if(args.length > 0) {
      return `@${directive.name}(${args.join(', ')})`
    } else {
      return `@${directive.name}`
    }
  }

  /**
   * Merges directives by summarizing arguments of
   * directives with equal name.
   */
  protected mergeDirectives(directives: IDirectiveInfo[]): IDirectiveInfo[] {
    // Group by name
    const grouped = directives.reduce((r, a) => {
      r[a.name] = r[a.name] || []
      r[a.name].push(a)
      return r
    }, {});

    const merged: IDirectiveInfo[] = []

    // Merge with same name
    for(const name of Object.keys(grouped)) {
      merged.push({
        name,
        arguments: grouped[name].reduce((r, a) => {
          return {...a.arguments, ...r.arguments}
        }, {})
      })
    }

    return merged
  }

  protected extractTypeIdentifier(type: string | IGQLType) {
    if(typeof type === 'string') {
      return type
    } else {
      return type.name
    }
  }

  protected renderValue(type: string | IGQLType, value: any) {
    const strType = this.extractTypeIdentifier(type)
    if (
      strType === TypeIdentifiers.string ||
      strType === TypeIdentifiers.json ||
      strType === TypeIdentifiers.dateTime
    ) {
      return `"${value}"`
    } else {
      return value
    }
  }
}