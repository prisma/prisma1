import { Project, ProjectInfo } from '../types'
import {
  deletingProjectMessage,
  deletedProjectMessage,
  deletingProjectWarningMessage,
  deletingProjectsMessage
} from '../utils/constants'
import * as chalk from 'chalk'
import figures = require('figures')
import * as _ from 'lodash'
import out from '../io/Out'
import env from '../io/Environment'
import client from '../io/Client'
import { generateErrorOutput, parseErrors } from '../utils/errors'

const {terminal} = require('terminal-kit')

export interface DeleteProps {
  projectId: string
}

export interface DeleteCliProps {
  project?: string
  env?: string
}

export default async (props: DeleteProps): Promise<void> => {
  if (props.projectId) {
    out.startSpinner(deletingProjectMessage(props.projectId))

    try {
      await client.deleteProjects([props.projectId])
      out.stopSpinner()
      out.write(deletedProjectMessage([props.projectId]))

    } catch (e) {
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

    const projects = await client.fetchProjects()
    terminal.saveCursor()
    terminal.grabInput()
    terminal.hideCursor()
    terminal(`\n`)

    // initially select current project
    const projectId = env.default ? env.default.projectId : null
    let currentIndex = projectId ? projects.map(p => p.id).indexOf(projectId) : 0
    const selectedIndices = []

    render(projects, currentIndex, selectedIndices)

    await new Promise(resolve => {
      terminal.on('key', async (name: string) => {
        currentIndex = await handleKeyEvent(name, currentIndex, selectedIndices, projects, resolve)
      })
    })
  }
}

async function handleKeyEvent(name: string,
                              currentIndex: number,
                              selectedIndices: number[],
                              projects: Project[],
                              callback: () => void): Promise<number> {

  switch (name) {
    case 'DOWN': {
      currentIndex = (currentIndex + 1) % projects.length
      rerender(projects, currentIndex, selectedIndices)
      break
    }
    case 'UP': {
      currentIndex = (currentIndex + projects.length - 1) % projects.length
      rerender(projects, currentIndex, selectedIndices)
      break
    }
    case ' ': { // SPACE
      const index = selectedIndices.indexOf(currentIndex)
      if (index >= 0) {
        selectedIndices.splice(index, 1)
      } else {
        selectedIndices.push(currentIndex)
      }
      rerender(projects, currentIndex, selectedIndices)
      break
    }
    case 'ENTER': {
      await handleSelect(selectedIndices, projects)
      terminal.grabInput(false)
      callback()
      break
    }
    case 'CTRL_C': {
      clear(projects)
      terminal.hideCursor(false)
      terminal.grabInput(false)
      process.exit()
    }
    default: {
      break
    }
  }

  return currentIndex
}

async function handleSelect(selectedIndices: number[], projects: Project[]): Promise<void> {

  terminal(`\n\n${deletingProjectWarningMessage}`)

  terminal.grabInput(true)
  await new Promise(resolve => {
    terminal.on('key', function (name) {
      if (name !== 'y') {
        process.exit(0)
      }
      terminal.grabInput(false)
      resolve()
    })
  })

  const projectIdsToDelete = selectedIndices.reduce((prev: string[], current: number) => {
    prev.push(projects[current].id)
    return prev
  }, [])

  terminal.restoreCursor()
  terminal.eraseDisplayBelow()
  terminal.hideCursor(false)
  out.startSpinner(deletingProjectsMessage(projectIdsToDelete))

  try {
    await client.deleteProjects(projectIdsToDelete)
    out.stopSpinner()
    out.write(deletedProjectMessage(projectIdsToDelete))

  } catch (e) {
    out.stopSpinner()

    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      out.writeError(`${output}`)
    } else {
      throw e
    }
  }

}

function rerender(projects: Project[], currentIndex: number, selectedIndices: number[]): void {
  clear(projects)
  render(projects, currentIndex, selectedIndices)
}

function clear(projects: Project[]) {
  const lineCount = _.flatten(projects).length - 1
  terminal.up(lineCount)
  terminal.left(10000)
  terminal.eraseDisplayBelow()
}

function render(projects: Project[], currentIndex: number, selectedIndices: number[]) {

  const lines = _.chain(projects)
    .map(project => `${project.name} (${project.id})`)
    .map((l, lineIndex) => (selectedIndices.includes(lineIndex)) ? `${chalk.red(figures.circleFilled)}  ${chalk.red(l)}` : `${figures.circle}  ${l}`)
    .map((l, lineIndex) => (lineIndex === currentIndex) ? `${figures.pointer} ${l}` : `  ${l}`)
    .join('\n')

  terminal(lines, currentIndex)
}
