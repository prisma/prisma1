import chalk from 'chalk'

export class ClusterNotFoundError extends Error {
  constructor(cluster: string) {
    const localNote =
      cluster === 'local'
        ? `You can boot the local cluster with ${chalk.bold.green(
            'graphcool local start',
          )}`
        : 'If it is a private cluster, you can use the graphcool cluster add command to add the cluster.'
    super(`The cluster ${cluster} is neither a known public cluster, nor included in the global ~/.graphcoolrc file.
${localNote}`)
  }
}
