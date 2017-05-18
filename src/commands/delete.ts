import {SystemEnvironment, ProjectInfo} from '../types'
import { deleteProject, fetchProjects, parseErrors, generateErrorOutput } from '../api/api'
import {
  deletingProjectMessage,
  deletedProjectMessage,
  deletingProjectWarningMessage,
  deletingProjectsMessage
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
      await deleteProject([props.sourceProjectId], resolver)
      out.stopSpinner()
      out.write(deletedProjectMessage([props.sourceProjectId]))

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
    terminal.saveCursor()
    terminal.grabInput()
    terminal.hideCursor()
    terminal(`\n`)

    let currentIndex = 0, selectedIndices = []

    render(projects, currentIndex, selectedIndices)

    await new Promise(resolve => {
      terminal.on('key', async (name: string) => {
        currentIndex = await handleKeyEvent(name, currentIndex, selectedIndices, projects, env, resolve)
      })
    })
  }

}

async function handleKeyEvent(
  name: string,
  currentIndex: number,
  selectedIndices: number[],
  projects: ProjectInfo[],
  env: SystemEnvironment,
  callback: () => void
): Promise<number> {

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
      await handleSelect(selectedIndices, projects, env)
      terminal.grabInput(false)
      callback()
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

async function handleSelect(selectedIndices: number[], projects: ProjectInfo[], env: SystemEnvironment): Promise<void> {

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
    prev.push(projects[current].projectId)
    return prev
  }, [])

  terminal.restoreCursor()
  terminal.eraseDisplayBelow()
  terminal.hideCursor(false)
  env.out.startSpinner(deletingProjectsMessage(projectIdsToDelete))

  try {
    await deleteProject(projectIdsToDelete, env.resolver)
    env.out.stopSpinner()
    env.out.write(deletedProjectMessage(projectIdsToDelete))

  } catch(e) {
    env.out.stopSpinner()

    if (e.errors) {
      const errors = parseErrors(e)
      const output = generateErrorOutput(errors)
      env.out.writeError(`${output}`)
    } else {
      throw e
    }
  }

}



function rerender(projects: ProjectInfo[], currentIndex: number, selectedIndices: number[]): void {
  clear(projects)
  render(projects, currentIndex, selectedIndices)
}

function clear(projects: ProjectInfo[]) {
  const lineCount = _.flatten(projects).length - 1
  terminal.up(lineCount)
  terminal.left(10000)
  terminal.eraseDisplayBelow()
}

function render(projects: ProjectInfo[], currentIndex: number, selectedIndices: number[]) {

  const lines = _.chain(projects)
    .map(project => `${project.name} (${project.projectId})`)
    .map((l, lineIndex) => (selectedIndices.indexOf(lineIndex) >= 0) ? `${chalk.red(figures.circleFilled)}  ${chalk.red(l)}` : `${figures.circle}  ${l}`)
    .map((l, lineIndex) => (lineIndex === currentIndex) ? `${chalk.bold(l)}` : `${l}`)
    .join('\n')

  terminal(lines, currentIndex)
}


