import { Observable } from 'zen-observable'
import { $$asyncIterator } from 'iterall'
type Callback = (value?: any) => any

export function observableToAsyncIterable<T>(
  observable: Observable<T>,
): AsyncIterator<T> {
  const pullQueue: Callback[] = []
  const pushQueue: any[] = []

  let listening = true

  const pushValue = ({ data }: any) => {
    if (pullQueue.length !== 0) {
      pullQueue.shift()!({ value: data, done: false })
    } else {
      pushQueue.push({ value: data })
    }
  }

  const pushError = (error: any) => {
    if (pullQueue.length !== 0) {
      pullQueue.shift()!({ value: { errors: [error] }, done: false })
    } else {
      pushQueue.push({ value: { errors: [error] } })
    }
  }

  const pullValue = () => {
    return new Promise(resolve => {
      if (pushQueue.length !== 0) {
        const element = pushQueue.shift()
        // either {value: {errors: [...]}} or {value: ...}
        resolve({
          ...element,
          done: false,
        })
      } else {
        pullQueue.push(resolve)
      }
    })
  }

  const subscription = observable.subscribe({
    next(value: any) {
      pushValue(value)
    },
    error(err: Error) {
      pushError(err)
    },
  })

  const emptyQueue = () => {
    if (listening) {
      listening = false
      subscription.unsubscribe()
      pullQueue.forEach(resolve => resolve({ value: undefined, done: true }))
      pullQueue.length = 0
      pushQueue.length = 0
    }
  }

  return {
    async next() {
      return listening ? pullValue() : this.return()
    },
    return() {
      emptyQueue()
      return Promise.resolve({ value: undefined, done: true })
    },
    throw(error) {
      emptyQueue()
      return Promise.reject(error)
    },
    [$$asyncIterator]() {
      return this
    },
  } as any
}
