import { Observable } from 'zen-observable';
export declare function observableToAsyncIterable<T>(observable: Observable<T>): AsyncIterator<T>;
