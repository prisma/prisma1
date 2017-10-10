export class InvalidTargetError extends Error {
  constructor() {
    super(
      `Please provide a valid target that points to a valid cluster and service id`,
    )
  }
}
