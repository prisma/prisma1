export class ClusterNotSet extends Error {
  constructor() {
    super(
      `No cluster set. In order to run this command, please set the "cluster" property in your graphcool.yml`,
    )
  }
}
