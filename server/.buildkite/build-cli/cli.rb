require_relative './src/build_context'
require_relative './src/command'
require_relative './src/commands'

Dir.chdir(__dir__)

context = BuildContext.new

# unless context.should_build?
#   puts "Nothing to do"
#   exit 0
# end

def print_usage
  puts """Prisma Build Tool
Usage: cli <subcommand>

Subcommands:
\tpipeline
\t\tRenders the pipeline based on the current build context and uploads it to buildkite.

\ttest <project> <connector>
\t\tTests given sbt project against the given connector.
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
  # ...

else
  puts "Invalid command: #{command}"
  exit 1
end