package util

object EnvVars {
  val serverRoot = sys.env
    .get("SERVER_ROOT")
    .orElse(sys.env.get("BUILDKITE_BUILD_CHECKOUT_PATH").map(path => s"$path/server")) // todo change as soon as the split is done
    .getOrElse(sys.error("Unable to resolve cargo root path"))

  val prismaBinaryPath: String = sys.env.getOrElse(
    "PRISMA_BINARY_PATH",
    sys.error("Required PRISMA_BINARY_PATH env var not found")
  )

  val migrationEngineBinaryPath: String = sys.env.getOrElse(
    "MIGRATION_ENGINE_BINARY_PATH",
    sys.error("Required MIGRATION_ENGINE_BINARY_PATH env var not found")
  )

  val isBuildkite = sys.env.get("IS_BUILDKITE").isDefined
}
