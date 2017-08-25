// TODO reenable when we have the interactive project flow defined
// import { CheckAuth, SystemEnvironment } from '../types'
// import { howDoYouWantToGetStarted, sampleSchemaURL } from '../utils/constants'
// import * as chalk from 'chalk'
// import figures = require('figures')
// import * as _ from 'lodash'
// import initCommand from './init'
// import { readProjectIdFromProjectFile } from '../utils/file'
// import { checkAuth } from '../utils/auth'
// import definition from '../io/ProjectDefinition/ProjectDefinition'
// import out from '../io/Out'
// const {terminal} = require('terminal-kit')
// const debug = require('debug')('graphcool')
//
// const BLANK_PROJECT = 0
//
// export interface InteractiveInitProps {
//   name?: string
//   alias?: string
//   region?: string
//   outputPath?: string
// }
//
// export default async (props: InteractiveInitProps): Promise<void> => {
//
//   let options = [
//     [`${chalk.bold('New blank project')}`, `Creates a new Graphcool project from scratch.`, ''],
//   ]
//
//   if (definition.definition) {
//     options.push(
//       [`${chalk.bold('New environment based on definition')}`, `Creates a new Graphcool project environment based on the graphcool.yml definition`, '']
//     )
//   }
//
//   terminal.saveCursor()
//   out.write(howDoYouWantToGetStarted())
//
//   terminal.grabInput()
//   terminal.hideCursor()
//
//   let currentIndex = BLANK_PROJECT // 0
//
//   render(options, currentIndex)
//
//   await new Promise(resolve => {
//     terminal.on('key', async (name: string) => {
//       currentIndex = await handleKeyEvent(name, currentIndex, options, props, resolve)
//     })
//   })
// }
//
// async function handleKeyEvent(name: string, currentIndex: number, options: string[][], props: InteractiveInitProps, callback: () => void): Promise<number> {
//
//   switch (name) {
//     case 'DOWN': {
//       currentIndex = (currentIndex + 1) % options.length
//       rerender(options, currentIndex)
//       break
//     }
//     case 'UP': {
//       currentIndex = (currentIndex + options.length - 1) % options.length
//       rerender(options, currentIndex)
//       break
//     }
//     case 'ENTER': {
//       await handleSelect(currentIndex, props)
//       callback()
//       break
//     }
//     case 'CTRL_C': {
//       terminal.restoreCursor()
//       terminal.eraseDisplayBelow()
//       terminal.hideCursor(false)
//       out.write('\n')
//       process.exit()
//     }
//     default: {
//       break
//     }
//   }
//
//   return currentIndex
// }
//
// async function handleSelect(selectedIndex: number, props: InteractiveInitProps): Promise<void> {
//   terminal.restoreCursor()
//   terminal.eraseDisplayBelow()
//   terminal.hideCursor(false)
//   out.write('\n')
//
//   if (selectedIndex === BLANK_PROJECT) {
//     terminal.grabInput(false)
//
//     await checkAuth('init')
//   }
//
//   switch (selectedIndex) {
//     case BLANK_PROJECT: {
//       const schemaUrl = sampleSchemaURL
//       const initProps = getPropsForInit(props)
//       await initCommand({...initProps, blank: true})
//       break
//     }
//     default: {
//       terminal.grabInput(false)
//       const previousOptions = 1
//       if (selectedIndex >= previousOptions && selectedIndex < previousOptions + schemaFiles.length) {
//         const schemaFileIndex = selectedIndex - previousOptions
//         const schemaUrl = schemaFiles[schemaFileIndex]
//         const initProps = getPropsForInit(props)
//         await initCommand({...initProps, schemaUrl}, env)
//       } else if (selectedIndex >= previousOptions + schemaFiles.length && selectedIndex < previousOptions + schemaFiles.length + projectFiles.length) {
//         const projectFileIndex = selectedIndex - schemaFiles.length - previousOptions
//         const projectFile = projectFiles[projectFileIndex]
//         const copyProjectId = readProjectIdFromProjectFile(env.resolver, projectFile)
//         const initProps = getPropsForInit(props)
//         const initProps2 = {...initProps, copyProjectId, projectFile}
//         await initCommand(initProps2, env)
//       }
//
//       break
//     }
//   }
// }
//
// function getPropsForInit(props: InteractiveInitProps): any {
//   return {
//     name: props.name,
//     alias: props.alias,
//     region: props.region,
//     outputPath: props.outputPath
//   }
// }
//
// function rerender(options: string[][], currentIndex: number): void {
//   clear(options)
//   render(options, currentIndex)
// }
//
// function clear(options: string[][]) {
//   const lineCount = _.flatten(options).length - 1
//   terminal.up(lineCount)
//   terminal.left(10000)
//   terminal.eraseDisplayBelow()
// }
//
// function render(options: string[][], currentIndex: number) {
//   const lines = _.chain(options)
//     .map((ls, optionIndex) => ls.map((l, lineIndex) => (lineIndex === 0 && optionIndex === currentIndex) ? `  ${chalk.blue(figures.pointer)} ${l}` : `    ${l}`))
//     .flatten()
//     .join('\n')
//
//   terminal(lines, currentIndex)
// }
