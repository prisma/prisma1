export function omitDeep(obj, key) {
  if (typeof obj === 'object') {
    return Object.keys(obj).reduce((acc, curr) => {
      if (curr === key) {
        return acc
      }
      return { ...acc, [curr]: omitDeep(obj[curr], key) }
    }, {})
  }

  return obj
}
