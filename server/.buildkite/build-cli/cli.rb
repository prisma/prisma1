require_relative './src/build_context'
require_relative './src/command'
require_relative './src/commands'

Dir.chdir(__dir__)

git_fetch
context = BuildContext.new

unless context.should_build?
  puts "Nothing to do"
  exit 0
end

def print_usage
  puts """Prisma Build Tool
Usage: cli <subcommand>

Subcommands:
\tpipeline
\t\tRenders the pipeline based on the current build context and uploads it to buildkite.

\ttest <project> <connector>
\t\tTests given sbt project against the given connector.

\tbuild <tag>
\t\tBuilds and tags the docker image(s) on the current branch with the given tag. Additional tags to process are inferred from the given tag.

\tnative-image <target> <version>
\t\tBuilds the native image on the current branch. Artifacts are always published to S3. <version> is the version string to be baked into the binary.
  """
end

if ARGV.length <= 0
  print_usage
  exit 1
end

command = ARGV[0]

case command
when "pipeline"
  upload_pipeline(context)

when "test"
  if ARGV.length <= 1
    print_usage
    exit 1
  end

  project = ARGV[1]
  if ARGV[2].nil?
    connector = :none
  else
    connector = ARGV[2].to_sym
  end

  test_project(context, project, connector)

when "build"
  if ARGV.length < 1
    print_usage
    exit 1
  end

  build_images(context, Tag.new(ARGV[1]))

when "native-image"
  if ARGV.length < 2
    print_usage
    exit 1
  end

  native_image(context, ARGV[1], ARGV[2])

else
  puts "Invalid command: #{command}"
  exit 1
end