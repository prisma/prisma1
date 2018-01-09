export class NoClusterSetError extends Error {
  constructor() {
    super(
      `No cluster set. Please set the "cluster" property in your graphcool.yml`,
    )
  }
}
