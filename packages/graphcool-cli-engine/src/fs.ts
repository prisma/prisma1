import * as fse from 'fs-extra'
// import { fs as memfs, vol } from 'memfs'
//
// if (process.env.NODE_ENV === 'test') {
//   reset()
// }
//
// const fs = process.env.NODE_ENV === 'test' ? memfs : fse

export default fse
//
// export function reset() {
//   vol.reset()
//   vol.fromJSON({
//     'test.out': ''
//   }, process.cwd())
// }
