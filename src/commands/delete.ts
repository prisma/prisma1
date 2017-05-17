import {SystemEnvironment} from '../types'
import { deleteProject, fetchProjects, parseErrors, generateErrorOutput } from '../api/api'
import {
  deletingProjectMessage,
  deletedProjectMessage
} from '../utils/constants'
import * as chalk from 'chalk'
import figures = require('figures')
import * as _ from 'lodash'
const {terminal} = require('terminal-kit')
const debug = require('debug')('graphcool')


interface Props {
  sourceProjectId?: string
}

export default async (props: Props, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  if (props.sourceProjectId) {
    out.startSpinner(deletingProjectMessage(props.sourceProjectId))

    try {
      await deleteProject(props.sourceProjectId, resolver)
      out.stopSpinner()
      out.write(deletedProjectMessage)

    } catch(e) {
      out.stopSpinner()

      if (e.errors) {
        const errors = parseErrors(e)
        const output = generateErrorOutput(errors)
        out.writeError(`${output}`)
      } else {
        throw e
      }
    }
  } else {

    const projects = await fetchProjects(resolver)
    const lines = projects.map(project => {
      return `${project.name} (${project.projectId})`
    })
    out.write(`\n`)
    terminal.saveCursor()
    terminal.grabInput()
    terminal.hideCursor()

    let currentIndex = 0, selectedIndices = []

    render(lines, currentIndex, selectedIndices)

    await new Promise(resolve => {
      terminal.on('key', async (name: string) => {
        currentIndex = await handleKeyEvent(name, currentIndex, selectedIndices, lines, env, resolve)
      })
    })
  }

}

async function handleKeyEvent(
  name: string,
  currentIndex: number,
  selectedIndices: number[],
  options: string[],
  env: SystemEnvironment,
  callback: () => void
): Promise<number> {

  switch (name) {
    case 'DOWN': {
      currentIndex = (currentIndex + 1) % options.length
      rerender(options, currentIndex, selectedIndices)
      break
    }
    case 'UP': {
      currentIndex = (currentIndex + options.length - 1) % options.length
      rerender(options, currentIndex, selectedIndices)
      break
    }
    case 'ENTER': {
      const index = selectedIndices.indexOf(currentIndex)
      if (index >= 0) {
        selectedIndices.splice(index, 1)
      } else {
        selectedIndices.push(currentIndex)
      }
      rerender(options, currentIndex, selectedIndices)
      break
    }
    case 'CTRL_C': {
      terminal.restoreCursor()
      terminal.eraseDisplayBelow()
      terminal.hideCursor(false)
      env.out.write('\n')
      process.exit()
    }
    default: {
      break
    }
  }

  return currentIndex
}

async function handleSelect(selectedIndex: number, env: SystemEnvironment): Promise<void> {
  terminal.restoreCursor()
  terminal.eraseDisplayBelow()
  terminal.hideCursor(false)
  env.out.write('\n')

    // if (selectedIndex === BLANK_PROJECT) {
    //   terminal.grabInput(false)
    //
    //   await props.checkAuth('init')
    // }
    //
    // switch (selectedIndex) {
    //   case BLANK_PROJECT: {
    //     const schemaUrl = sampleSchemaURL
    //     const initProps = getPropsForInit(props)
    //     await initCommand({...initProps, schemaUrl}, env)
    //     break
    //   }
    //   default: {
    //     terminal.grabInput(false)
    //     const schemaFiles = env.resolver.schemaFiles('.')
    //     const projectFiles = env.resolver.projectFiles('.')
    //     const previousOptions = 1
    //     if (selectedIndex >= previousOptions && selectedIndex < previousOptions + schemaFiles.length) {
    //       const schemaFileIndex = selectedIndex - previousOptions
    //       const localSchemaFile = schemaFiles[schemaFileIndex]
    //       const initProps = getPropsForInit(props)
    //       await initCommand({...initProps, localSchemaFile}, env)
    //     } else if (selectedIndex >= previousOptions + schemaFiles.length && selectedIndex < previousOptions + schemaFiles.length + projectFiles.length) {
    //       const projectFileIndex = selectedIndex - schemaFiles.length - previousOptions
    //       const projectFile = projectFiles[projectFileIndex]
    //       const copyProjectId = readProjectIdFromProjectFile(env.resolver, projectFile)
    //       const initProps = getPropsForInit(props)
    //       const initProps2 = {...initProps, copyProjectId, projectFile}
    //       await initCommand(initProps2, env)
    //     }
    //
    //     break
    //   }
    // }
}



function rerender(options: string[], currentIndex: number, selectedIndices: number[]): void {
  clear(options)
  render(options, currentIndex, selectedIndices)
}

function clear(options: string[]) {
  const lineCount = _.flatten(options).length - 1
  terminal.up(lineCount)
  terminal.left(10000)
  terminal.eraseDisplayBelow()
}

function render(projects: string[], currentIndex: number, selectedIndices: number[]) {
  const lines = _.chain(projects)
    .map((l, lineIndex) => (selectedIndices.indexOf(lineIndex) >= 0) ? `${chalk.red(figures.circleFilled)}  ${l}` : `${figures.circle}  ${l}`)
    .map((l, lineIndex) => (lineIndex === currentIndex) ? `${chalk.bold(l)}` : `${l}`)
    .join('\n')

  terminal(lines, currentIndex)
}


