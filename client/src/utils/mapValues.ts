export function mapValues(obj, cb) {
  const newObj = {}
  Object.entries(obj).forEach(([key, value]) => {
    newObj[key] = cb(key, value)
  })
  return newObj
}
