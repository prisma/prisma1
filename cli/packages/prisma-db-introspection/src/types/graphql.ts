import * as os from 'os'
import * as _ from 'lodash'

// This class is deprecated and only there for reference. 
class GQLType {
  name: string
  fields: GQLField[]
  directives: string[]
  renderCommented: boolean

  constructor(name: string, fields: GQLField[], directives: string[], renderCommented: boolean) {
    this.name = name
    this.fields = fields
    this.directives = directives
    this.renderCommented = renderCommented
  }

  // Determines if this type is valid GraphQL compatible with Prisma
  isValid(): boolean {
    return this.fields.some(f => f.isValid())
  }

  render(): string {
    const orderFields = (fields: GQLField[]): GQLField[] => {
      return fields.sort((a, b) => {
        if (a.name < b.name) { return -1 }
        if (a.name > b.name) { return 1 }
        return 0
      })
    }

    // Dissect fields
    const idField = this.fields.find(x => x.isIdField) || null
    const relationFields: GQLField[] = [] // orderFields
    const scalarFields = orderFields(this.fields.filter(f => !f.isIdField))

    // Render fields
    const renderedFields = scalarFields.map(f => f.render())
    if (idField !== null) {
      renderedFields.unshift(idField.render())
    }

    relationFields.map(f => f.render()).forEach(r => renderedFields.push(r))

    // Render type
    return `${this.renderCommented ? "# " : ""}type ${capitalizeFirstLetter(this.name)} ${this.directives.join(" ")} {
${renderedFields.join(os.EOL)}
${this.renderCommented ? "# " : ""}}`
  }
}

class GQLField {
  name: string
  type: string
  isRequired: boolean
  directives: string[]
  isIdField: boolean
  comment: string
  renderCommented: boolean

  constructor(name: string, type: string, isRequired: boolean, directives: string[],
    isIdField: boolean, comment: string, renderCommented: boolean) {
    this.name = name
    this.type = type
    this.isRequired = isRequired
    this.directives = directives
    this.isIdField = isIdField
    this.comment = comment
    this.renderCommented = renderCommented
  }

  isValid(): boolean {
    // In current context, commented fields are commented only if they are invalid
    return !this.renderCommented
  }

  render(): string {
    const prefix = (!this.isValid() || this.renderCommented) ? "# " : ""
    const suffix = (this.comment.length > 0) ? ` # ${this.comment}` : ''
    const directives = this.directives.join(" ")
    const directivesString = directives.trim().length > 0 ? ` ${directives}` : '';
    return `  ${prefix}${this.name}: ${this.type}${this.isRequired ? '!' : ''}${directivesString}${suffix}`
  }
}

export class SDL {
  types: GQLType[]

  constructor(types: GQLType[]) {
    this.types = types
  }

  render(): string {
    const orderedTypes = _.sortBy(this.types, ['name'])
    return orderedTypes.map(t => t.render()).join("\n\n")
  }
}

// Utilities
function capitalizeFirstLetter(string) {
  return string.charAt(0).toUpperCase() + string.slice(1)
}