export type GeneratorType = 'typescript' | 'javascript'

export type Interpolation<P> =
  | FlattenInterpolation<P>
  | ReadonlyArray<
      FlattenInterpolation<P> | ReadonlyArray<FlattenInterpolation<P>>
    >
export type FlattenInterpolation<P> =
  | InterpolationValue
  | InterpolationFunction<P>
export type InterpolationValue = string | number | boolean
export type SimpleInterpolation =
  | InterpolationValue
  | ReadonlyArray<InterpolationValue | ReadonlyArray<InterpolationValue>>
export interface InterpolationFunction<P> {
  (props: P): Interpolation<P>
}
