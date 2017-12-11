export class ClusterNotFound extends Error {
  constructor(name: string) {
    super(
      `Cluster '${name}' is neither a known shared cluster nor defined in your global .graphcoolrc.`,
    )
  }
}
