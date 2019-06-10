export interface ListMap<T> {
  [x: string]: T[]
}

export interface Map<T> {
  [x: string]: T
}

export function aggregateBy<T>(data: Array<T>, fn: (T) => string): ListMap<T> {
  const aggregate = {} as ListMap<T>

  for (const d of data) {
    const key = fn(d)
    const list = aggregate[key]
    if (!list) {
      aggregate[key] = [d]
    } else {
      list.push(d)
    }
  }

  return aggregate
}

export function aggregateFlatBy<T>(data: Array<T>, fn: (T) => string): Map<T> {
  const aggregate = {} as Map<T>

  for (const d of data) {
    const key = fn(d)
    aggregate[key] = d
  }

  return aggregate
}
