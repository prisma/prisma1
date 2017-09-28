import { Output } from './index'
import * as stripAnsi from 'strip-ansi'

export interface TableColumn<T> {
  key: string,
  label?: string,
  format: (value: string, row: string) => string,
  get: (row: T) => string,
  width: number
}

export interface TableOptions<T> {
  columns?: Array<TableColumn<T>>,
  after: (row: T, options: TableOptions<T>) => void,
  printRow: (row: any[]) => void,
  printHeader?: (row: any[]) => void
}

/**
 * Generates a Unicode table and feeds it into configured printer.
 *
 * Top-level arguments:
 *
 * @arg {Object[]} data - the records to format as a table.
 * @arg {Object} options - configuration for the table.
 *
 * @arg {Object[]} [options.columns] - Options for formatting and finding values for table columns.
 * @arg {function(string)} [options.headerAnsi] - Zero-width formattter for entire header.
 * @arg {string} [options.colSep] - Separator between columns.
 * @arg {function(row, options)} [options.after] - Function called after each row is printed.
 * @arg {function(string)} [options.printLine] - Function responsible for printing to terminal.
 * @arg {function(cells)} [options.printHeader] - Function to print header cells as a row.
 * @arg {function(cells)} [options.printRow] - Function to print cells as a row.
 *
 * @arg {function(row)|string} [options.columns[].key] - Path to the value in the row or function to retrieve the pre-formatted value for the cell.
 * @arg {function(string)} [options.columns[].label] - Header name for column.
 * @arg {function(string, row)} [options.columns[].format] - Formatter function for column value.
 * @arg {function(row)} [options.columns[].get] - Function to return a value to be presented in cell without formatting.
 *
 */
function table<T = { height?: number }>(out: Output, data: any[], options: TableOptions<T>) {
  const ary = require('lodash.ary')
  const defaults = require('lodash.defaults')
  const get = require('lodash.get')
  const identity = require('lodash.identity')
  const partial = require('lodash.partial')
  const property = require('lodash.property')
  const result = require('lodash.result')

  const defaultOptions = {
    colSep: '  ',
    after: () => {
      // noop
    },
    headerAnsi: identity,
    printLine: s => out.log(s),
    printRow(cells) {
      this.printLine(cells.join(this.colSep).trimRight())
    },
    printHeader(cells) {
      this.printRow(cells.map(ary(this.headerAnsi, 1)))
      this.printRow(cells.map(hdr => hdr.replace(/./g, '─')))
    }
  }

  const colDefaults = {
    format: value => value ? value.toString() : '',
    width: 0,
    label() {
      return this.key.toString()
    },

    get (row) {
      const path = result(this, 'key')
      const value = !path ? row : get(row, path)
      return this.format(value, row)
    }
  }

  function calcWidth(cell) {
    const lines = stripAnsi(cell).split(/[\r\n]+/)
    const lineLengths = lines.map(property('length'))
    return Math.max.apply(Math, lineLengths)
  }

  function pad(str: string, length: number) {
    const visibleLength = stripAnsi(str).length
    const diff = length - visibleLength

    return str + ' '.repeat(Math.max(0, diff))
  }

  function render() {
    let columns: Array<TableColumn<T>> = options.columns || Object.keys((data[0] as any) || {}) as any

    if (typeof columns[0] === 'string') {
      columns = (columns as any).map(key => ({key}))
    }

    let defaultsApplied = false
    for (const row of data) {
      row.height = 1
      for (const col of columns) {
        if (!defaultsApplied) {
          defaults(col, colDefaults)
        }

        const cell = col.get(row)

        col.width = Math.max(
          result(col, 'label').length,
          col.width || 0,
          calcWidth(cell)
        )

        row.height = Math.max(
          row.height || 0,
          cell.split(/[\r\n]+/).length
        )
      }
      defaultsApplied = true
    }

    if (options.printHeader) {
      options.printHeader(columns.map((col) => {
        const label = result(col, 'label')
        return pad(label, col.width || label.length)
      }))
    }

    function getNthLineOfCell(n, row, col) {
      // TODO memoize this
      const lines = col.get(row).split(/[\r\n]+/)
      return pad(lines[n] || '', col.width)
    }

    for (const row of data) {
      for (let i = 0; i < (row.height || 0); i++) {
        const cells = columns.map(partial(getNthLineOfCell, i, row))
        options.printRow(cells)
      }
      options.after(row, options)
    }
  }

  defaults(options, defaultOptions)
  render()
}

module.exports = table
