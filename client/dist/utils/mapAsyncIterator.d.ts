/**
 * Note: Taken from https://raw.githubusercontent.com/apollographql/graphql-tools/2d5cba0e3edf89b99331d1c563c7c69f19ebac16/src/stitching/mapAsyncIterator.ts
 * as it's not exported. TODO: PR to graphql-tools to export this function
 */
/**
 * Given an AsyncIterable and a callback function, return an AsyncIterator
 * which produces values mapped via calling the callback function.
 */
export default function mapAsyncIterator<T, U>(iterator: AsyncIterator<T>, callback: (value: T) => Promise<U> | U, rejectCallback?: any): AsyncIterator<U>;
