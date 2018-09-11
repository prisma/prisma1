import { Interpolation } from '../types'

export const interleave = <Props>(
  strings: TemplateStringsArray,
  interpolations: Interpolation<Props>[],
): Interpolation<Props>[] =>
  interpolations.reduce(
    (array: Interpolation<Props>[], interp: Interpolation<Props>, i: number) =>
      array.concat(interp, strings[i + 1]),
    [strings[0]],
  )
