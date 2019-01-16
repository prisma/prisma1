import { Interpolation } from '../types'

const flatten = <T>(
  chunks: Interpolation<any>[],
  executionContext: T,
): Interpolation<T>[] =>
  chunks.reduce((ruleSet: Interpolation<T>[], chunk?: Interpolation<T>) => {
    /* Remove falsey values */
    if (
      chunk === undefined ||
      chunk === null ||
      chunk === false ||
      chunk === ''
    ) {
      return ruleSet
    }
    /* Flatten ruleSet */
    if (Array.isArray(chunk)) {
      return [...ruleSet, ...flatten(chunk, executionContext)]
    }

    /* Either execute or defer the function */
    if (typeof chunk === 'function') {
      return executionContext
        ? ruleSet.concat(
            ...flatten([chunk(executionContext)], executionContext),
          )
        : ruleSet.concat(chunk)
    }

    return ruleSet.concat(chunk.toString())
  }, [])

export default flatten
