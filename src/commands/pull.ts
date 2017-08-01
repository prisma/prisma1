import { SystemEnvironment, Resolver, ProjectInfo } from '../types'
import {
  readProjectIdFromProjectFile,
  writeProjectFile,
  readVersionFromProjectFile,
  isValidProjectFilePath
} from '../utils/file'
import { pullProjectInfo, parseErrors, generateErrorOutput, fetchProjects } from '../api/api'
import * as _ from 'lodash'
import {
  fetchingProjectDataMessage,
  wroteProjectFileMessage,
  newVersionMessage,
  differentProjectIdWarningMessage,
  invalidProjectFilePathMessage,
  graphcoolProjectFileName,
  multipleProjectFilesForPullMessage,
  pulledInitialProjectFileMessage,
  warnOverrideProjectFileMessage
} from '../utils/constants'
import figures = require('figures')

const {terminal} = require('terminal-kit')
const debug = require('debug')('graphcool')

export interface PullProps {
  sourceProjectId?: string
  projectFile?: string
  outputPath?: string
  force: boolean
}

export default async (props: PullProps, env: SystemEnvironment): Promise<void> => {
  const {resolver, out} = env

  try {

    const projectId = await getProjectId(props, env)
    const projectFile = props.projectFile || graphcoolProjectFileName
    const outputPath = props.outputPath || projectFile
    const currentVersion = getCurrentVersion(projectFile, resolver)

    // warn if the current project file is different from specified project id
    if (resolver.exists(graphcoolProjectFileName)) {
      const readProjectId = readProjectIdFromProjectFile(resolver, graphcoolProjectFileName)
      if (readProjectId && projectId !== readProjectId) {
        out.write(differentProjectIdWarningMessage(projectId!, readProjectId))
        terminal.grabInput(true)

        await new Promise(resolve => {
          terminal.on('key', name => {
            if (name !== 'y') {
              process.exit(0)
            }
            terminal.grabInput(false)
            resolve()
          })
        })
      }
    }

    if (!props.force && projectFile === outputPath && resolver.exists(projectFile)) {
      out.write(warnOverrideProjectFileMessage(projectFile))
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
    }

    out.startSpinner(`${fetchingProjectDataMessage}`)
    const projectInfo = await pullProjectInfo(projectId!, resolver)

    out.stopSpinner()

    const message = resolver.projectFiles('.').length === 0 ?
      pulledInitialProjectFileMessage(outputPath) :
      wroteProjectFileMessage(outputPath)

    writeProjectFile(projectInfo, resolver, outputPath)

    out.write(message)
    if (projectInfo.version && currentVersion) {
      const shouldDisplayVersionUpdate = parseInt(projectInfo.version!) > parseInt(currentVersion!)
      if (shouldDisplayVersionUpdate) {
        const message = newVersionMessage(projectInfo.version)
        out.write(` ${message}`)
      }
    }

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

function getProjectFilePath(props: PullProps, env: SystemEnvironment): string | undefined {
  const {resolver} = env

  // check if provided file is valid (ends with correct suffix)
  if (props.projectFile && isValidProjectFilePath(props.projectFile)) {
    return props.projectFile
  } else if (props.projectFile && !isValidProjectFilePath(props.projectFile)) {
    throw new Error(invalidProjectFilePathMessage(props.projectFile))
  }

  // no project file provided, search for one in current dir
  const projectFiles = resolver.projectFiles('.')
  if (projectFiles.length === 0) {
    return undefined
  } else if (projectFiles.length > 1) {
    throw new Error(multipleProjectFilesForPullMessage(projectFiles))
  }

  return projectFiles[0]
}

async function getProjectId(props: PullProps, env: SystemEnvironment): Promise<string> {
  if (props.sourceProjectId) {
    return props.sourceProjectId
  }

  const projectFile = getProjectFilePath(props, env)
  if (projectFile && env.resolver.exists(projectFile)) {
    const projectId = readProjectIdFromProjectFile(env.resolver, projectFile)
    if (projectId) {
      return projectId
    }
  }

  return interactiveProjectSelection(env)
}

function getCurrentVersion(path: string, resolver: Resolver): string | undefined {
  if (resolver.exists(path)) {
    return readVersionFromProjectFile(resolver, path)
  }
  return undefined
}

async function interactiveProjectSelection(env: SystemEnvironment): Promise<string> {
  const projects = await fetchProjects(env.resolver)
  terminal.saveCursor()
  terminal.grabInput()
  terminal.hideCursor()
  terminal(`\n`)

  let currentIndex = 0

  render(projects, currentIndex)

  const projectId = await new Promise<string>(resolve => {
    terminal.on('key', async (name: string) => {
      currentIndex = await handleKeyEvent(name, currentIndex, projects, resolve)
    })
  })

  return projectId
}

function rerender(projects: ProjectInfo[], currentIndex: number): void {
  clear(projects)
  render(projects, currentIndex)
}

function clear(projects: ProjectInfo[]) {
  const lineCount = _.flatten(projects).length - 1
  terminal.up(lineCount)
  terminal.left(10000)
  terminal.eraseDisplayBelow()
}

function render(projects: ProjectInfo[], currentIndex: number) {

  const lines = _.chain(projects)
    .map(project => `${project.name} (${project.projectId})`)
    .map((l, lineIndex) => (lineIndex === currentIndex) ? `${figures.pointer} ${l}` : `  ${l}`)
    .join('\n')

  terminal(lines, currentIndex)
}

async function handleKeyEvent(name: string,
                              currentIndex: number,
                              projects: ProjectInfo[],
                              callback: (projectId: string) => void): Promise<number> {

  switch (name) {
    case 'DOWN': {
      currentIndex = (currentIndex + 1) % projects.length
      rerender(projects, currentIndex)
      break
    }
    case 'UP': {
      currentIndex = (currentIndex + projects.length - 1) % projects.length
      rerender(projects, currentIndex)
      break
    }
    case 'ENTER': {
      clear(projects)
      terminal.hideCursor(false)
      terminal.grabInput(false)
      callback(projects[currentIndex].projectId)
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
