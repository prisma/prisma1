import { Interpolation } from '../types';
declare const flatten: <T>(chunks: Interpolation<any>[], executionContext: T) => Interpolation<T>[];
export default flatten;
