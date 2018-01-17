import { doJobs } from './StatusChecker'

const { request, cachePath } = JSON.parse(process.argv[2])

doJobs(cachePath, request)
