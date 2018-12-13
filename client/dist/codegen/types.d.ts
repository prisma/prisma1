export declare type GeneratorType = 'typescript' | 'javascript';
export declare type Interpolation<P> = FlattenInterpolation<P> | ReadonlyArray<FlattenInterpolation<P> | ReadonlyArray<FlattenInterpolation<P>>>;
export declare type FlattenInterpolation<P> = InterpolationValue | InterpolationFunction<P>;
export declare type InterpolationValue = string | number | boolean;
export declare type SimpleInterpolation = InterpolationValue | ReadonlyArray<InterpolationValue | ReadonlyArray<InterpolationValue>>;
export interface InterpolationFunction<P> {
    (props: P): Interpolation<P>;
}
