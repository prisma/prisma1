/* eslint-disable no-await-in-loop */
/* tslint:disable */
'use strict'

const inquirer = require('inquirer')

const isNumber = i => typeof i === 'number'
const isFunction = i => typeof i === 'function'
const isUndefined = i => typeof i === 'undefined'

/**
 * @param  {Object} prompt
 * @param  {Object} answers
 * @param  {string} input
 * @return {Promise.<string|string[]|Object>}
 */
async function promptHandler(prompt, answers, input) {
  if (prompt.when === false) {
    return
  }
  if (isFunction(prompt.when) && !(await prompt.when(answers))) {
    return
  }
  if (isFunction(prompt.message)) {
    // Just for coverage
    prompt.message(answers)
  }
  if (isFunction(prompt.transformer)) {
    // Just for coverage
    prompt.message(input)
  }

  let answer = input
  if (isUndefined(answer)) {
    if (isFunction(prompt.default)) {
      answer = await prompt.default(answers)
    } else {
      answer = prompt.default
    }
    if (isNumber(answer) && prompt.type in ['list', 'rawlist', 'expand']) {
      if (isFunction(prompt.choiches)) {
        answer = await prompt.choiches(answers)[answer]
      } else {
        answer = prompt.choiches[answer]
      }
    }
  }

  if (isUndefined(answer)) {
    switch (prompt.type) {
      case 'expand':
        answer = {
          key: 'h',
          name: 'Help, list all options',
          value: 'help',
        }
        break
      case 'checkbox':
        answer = []
        break
      case 'confirm':
        answer = false
        break
      default:
        if (Array.isArray(prompt.choiches)) {
          ;[answer] = prompt.choiches
        } else if (isFunction(prompt.choiches)) {
          ;[answer] = await prompt.choiches(answers)
        } else {
          answer = ''
        }
    }
  }

  if (isFunction(prompt.filter)) {
    answer = await prompt.filter(answer)
  }
  if (isFunction(prompt.validate)) {
    const valid = await prompt.validate(answer, answers)
    if (valid !== true) {
      throw new Error(valid)
    }
  }
  return answer
}

/**
 * @param  {Object} inputs
 * @return {Function}
 */
function inquirerHandler(inputs) {
  /**
   * @param  {Object} prompts
   * @return {Promise.<Object>}
   */
  return async prompts => {
    const answers = {}
    for (const prompt of [].concat(prompts) as any) {
      answers[prompt.name] = await promptHandler(
        prompt,
        answers,
        inputs[prompt.name],
      )
    }
    return answers
  }
}
/**
 * @param  {Object|Object[]} inputs
 */
export function mocki(inputs) {
  if (typeof inputs !== 'object') {
    throw new TypeError('The mocked answers must be an objects.')
  }

  const promptOriginal = inquirer.prompt
  const promptMock = async function(questions) {
    try {
      const answers = await inquirerHandler(inputs)(questions)
      inquirer.prompt = promptOriginal
      return Promise.resolve(answers)
    } catch (err) {
      inquirer.prompt = promptOriginal
      return Promise.reject(err)
    }
  } as any
  promptMock.prompts = inquirer.prompt.prompts
  promptMock.registerPrompt = inquirer.prompt.registerPrompt
  promptMock.restoreDefaultPrompts = inquirer.prompt.restoreDefaultPrompts
  // inquirer.prompt = promptMock
  // return inquirer
  return {
    prompt: promptMock,
  }
}
