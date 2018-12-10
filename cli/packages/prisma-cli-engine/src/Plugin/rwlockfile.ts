/* tslint:disable */
const fs = require('fs-extra')
const path = require('path')
const debug = require('debug')('rwlockfile')
const mkdir = require('mkdirp')

let locks = {}
let readers = {}

async function pidActive(pid: number): Promise<boolean> {
  if (!pid || isNaN(pid)) return false
  return process.platform === 'win32'
    ? pidActiveWindows(pid)
    : pidActiveUnix(pid)
}

function pidActiveWindows(pid: number): Promise<boolean> {
  const ps = require('ps-node')
  return new Promise((resolve, reject) => {
    ps.lookup({ pid }, (err, result) => {
      if (err) return reject(err)
      resolve(result.length > 0)
    })
  })
}

function pidActiveUnix(pid: number): boolean {
  try {
    return Boolean(process.kill(pid, 0))
  } catch (e) {
    return e.code === 'EPERM'
  }
}

async function lockActive(path: string): Promise<boolean> {
  try {
    let file = await readFile(path)
    let pid = parseInt(file.trim())
    let active = pidActive(pid)
    if (!active) debug(`stale pid ${path} ${pid}`)
    return active
  } catch (err) {
    if (err.code !== 'ENOENT') throw err
    return false
  }
}

function unlock(path: string) {
  return new Promise(resolve => fs.remove(path, resolve)).then(() => {
    delete locks[path]
  })
}

function wait(ms: number) {
  return new Promise(resolve => setTimeout(resolve, ms))
}

function unlockSync(path: string) {
  try {
    fs.removeSync(path)
  } catch (err) {
    debug(err)
  }
  delete locks[path]
}

function lock(p: string, timeout: number) {
  let pidPath = path.join(p, 'pid')
  if (!fs.existsSync(path.dirname(p))) mkdir.sync(path.dirname(p))
  return new Promise((resolve, reject) => {
    fs.mkdir(p, err => {
      if (!err) {
        locks[p] = 1
        fs.writeFile(pidPath, process.pid.toString(), resolve)
        return
      }
      if (err.code !== 'EEXIST') return reject(err)
      lockActive(pidPath)
        .then(active => {
          if (!active)
            return unlock(p)
              .then(resolve as any)
              .catch(reject)
          if (timeout <= 0) throw new Error(`${p} is locked`)
          debug(`locking ${p} ${timeout / 1000}s...`)
          wait(1000).then(() =>
            lock(p, timeout - 1000)
              .then(resolve)
              .catch(reject),
          )
        })
        .catch(reject)
    })
  })
}

function readFile(path: string): Promise<string> {
  return new Promise((resolve, reject) => {
    fs.readFile(path, 'utf8', (err, body) => {
      if (err) return reject(err)
      resolve(body)
    })
  })
}

function writeFile(path: string, content: string) {
  return new Promise((resolve, reject) => {
    fs.writeFile(path, content, (err, body) => {
      if (err) return reject(err)
      resolve(body)
    })
  })
}

async function getReadersFile(path): Promise<number[]> {
  try {
    let f = await readFile(path + '.readers')
    return f.split('\n').map(r => parseInt(r))
  } catch (err) {
    return []
  }
}

function getReadersFileSync(path): number[] {
  try {
    let f = fs.readFileSync(path + '.readers', 'utf8')
    return f.split('\n').map(r => parseInt(r))
  } catch (err) {
    return []
  }
}

const unlink = p =>
  new Promise((resolve, reject) =>
    fs.unlink(p, err => (err ? reject(err) : resolve())),
  )

function saveReaders(path, readers) {
  path += '.readers'
  if (readers.length === 0) {
    return unlink(path).catch(() => {})
  } else {
    return writeFile(path, readers.join('\n'))
  }
}

function saveReadersSync(path, readers) {
  path += '.readers'
  try {
    if (readers.length === 0) {
      fs.unlinkSync(path)
    } else {
      fs.writeFileSync(path, readers.join('\n'))
    }
  } catch (err) {}
}

async function getActiveReaders(
  path: string,
  timeout: number,
  skipOwnPid: boolean = false,
): Promise<number[]> {
  await lock(path + '.readers.lock', timeout)
  let readers: number[] = await getReadersFile(path)
  let promises = readers.map(r =>
    pidActive(r).then(active => (active ? r : null)),
  )
  let activeReaders = await Promise.all(promises)
  activeReaders = activeReaders.filter(r => r !== null)
  if (activeReaders.length !== readers.length) {
    await saveReaders(path, activeReaders)
  }
  await unlock(path + '.readers.lock')
  return skipOwnPid
    ? activeReaders.filter(r => r !== process.pid)
    : (activeReaders as any)
}

async function waitForReaders(
  path: string,
  timeout: number,
  skipOwnPid: boolean,
) {
  let readers = await getActiveReaders(path, timeout, skipOwnPid)
  if (readers.length !== 0) {
    if (timeout <= 0)
      throw new Error(
        `${path} is locked with ${
          readers.length === 1 ? 'a reader' : 'readers'
        } active: ${readers.join(' ')}`,
      )
    debug(`waiting for readers: ${readers.join(' ')} timeout=${timeout}`)
    await wait(1000)
    await waitForReaders(path, timeout - 1000, skipOwnPid)
  }
}

function waitForWriter(path, timeout) {
  return hasWriter(path).then(active => {
    if (active) {
      if (timeout <= 0)
        throw new Error(`${path} is locked with an active writer`)
      debug(`waiting for writer: path=${path} timeout=${timeout}`)
      return wait(1000).then(() => waitForWriter(path, timeout - 1000))
    }
    return unlock(path)
  })
}

export async function unread(path: string, timeout: number = 60000) {
  await lock(path + '.readers.lock', timeout)
  let readers = await getReadersFile(path)
  if (readers.find(r => r === process.pid)) {
    await saveReaders(path, readers.filter(r => r !== process.pid))
  }
  await unlock(path + '.readers.lock')
}

function unreadSync(path: string) {
  // TODO: potential lock issue here since not using .readers.lock
  let readers = getReadersFileSync(path)
  saveReadersSync(path, readers.filter(r => r !== process.pid))
}

export interface WriteLockOptions {
  timeout?: number
  skipOwnPid?: boolean
}

/**
 * lock for writing
 * @param path {string} - path of lockfile to use
 * @param options {object}
 * @param [options.timeout=60000] {number} - Max time to wait for lockfile to be open
 * @param [options.skipOwnPid] {boolean} - Do not wait on own pid (to upgrade current process)
 * @returns {Promise}
 */
export async function write(path: string, options: WriteLockOptions = {}) {
  let skipOwnPid = !!options.skipOwnPid
  let timeout = options.timeout || 60000
  debug(`write ${path}`)
  await waitForReaders(path, timeout, skipOwnPid)
  await lock(path + '.writer', timeout)
  return () => unlock(path + '.writer')
}

export interface ReadLockOptions {
  timeout: number
}

/**
 * lock for reading
 * @param path {string} - path of lockfile to use
 * @param options {object}
 * @param [options.timeout=60000] {number} - Max time to wait for lockfile to be open
 * @returns {Promise}
 */
export const read = async function(path: string, options: any = {}) {
  let timeout = options.timeout || 60000
  debug(`read ${path}`)
  await waitForWriter(path, timeout)
  await lock(path + '.readers.lock', timeout)
  let readersFile = await getReadersFile(path)
  await saveReaders(path, readersFile.concat([process.pid]))
  await unlock(path + '.readers.lock')
  readers[path] = 1
  return () => unread(path, timeout)
}

/**
 * check if active writer
 * @param path {string} - path of lockfile to use
 */
export async function hasWriter(p: string): Promise<boolean> {
  let pid
  try {
    pid = await readFile(path.join(p + '.writer', 'pid'))
  } catch (err) {
    if (err.code !== 'ENOENT') throw err
  }
  if (!pid) return false
  let active = await pidActive(parseInt(pid))
  return active
}

export async function hasReaders(
  p: string,
  options: WriteLockOptions = {},
): Promise<boolean> {
  let timeout = options.timeout || 60000
  let skipOwnPid = !!options.skipOwnPid
  let readers = await getActiveReaders(p, timeout, skipOwnPid)
  return readers.length !== 0
}

export function cleanup() {
  Object.keys(locks).forEach(unlockSync)
  Object.keys(readers).forEach(unreadSync)
}

process.once('exit', exports.cleanup)
