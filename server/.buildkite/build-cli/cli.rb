require_relative './src/build_context'
require_relative './src/command'
require_relative './src/commands'

class CLI
  def self.main(argv)
    context = BuildContext.new

    # unless context.should_build?
    #   puts "Nothing to do"
    #   exit 0
    # end

    if ARGV.length <= 0
      puts """Prisma Build Tool
    Usage: cli <subcommand>

    Subcommands:
    \tpipeline
    \t\tRenders the pipeline based on the current build context and uploads it to buildkite.

    \ttest <project> <connector>
    \t\tTests given sbt project against the given connector.
      """
      exit 1
    end

    command = ARGV[0]

    case command
    when "pipeline"
      upload_pipeline(context)

    when "test"
      #test_project(context)

    else
      puts "Invalid command: #{command}"
      exit 1
    end
  end
end