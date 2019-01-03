require_relative './pipeline_step'

class PipelineRenderer
  # Rules to generate the test excution
  # Valid connectors: :all, :none, :mongo, :mysql, :postgres
  # https://github.com/buildkite/emojis
  @@test_rules = {
    :'api-connector-mysql' => {
      :label => ":mysql: MySql API connector",
      :connectors => [:mysql]
    },
    :'deploy-connector-mysql' => {
      :label => ":mysql: MySql deploy connector",
      :connectors => [:mysql]
    },
    :'integration-tests-mysql' => {
      :label => ":slot_machine: Integration tests",
      :connectors => [:mysql, :postgres]
    },
    :'api-connector-postgres' => {
      :label => ":postgres: Postgres API connector",
      :connectors => [:postgres]
    },
    :'deploy-connector-postgres' => {
      :label => ":postgres: Postgres deploy connector",
      :connectors => [:postgres]
    },
    :'api-connector-mongo' => {
      :label => ":piedpiper: MongoDB API connector",
      :connectors => [:mongo]
    },
    :'deploy-connector-mongo' => {
      :label => ":piedpiper: MongoDB deploy connector",
      :connectors => [:mongo]
    },
    :'libs' => {
      :label => ":scala: Libraries",
      :connectors => [:none]
    },
    :'subscriptions' => {
      :label => ":scala: Subscriptions",
      :connectors => [:postgres]
    },
    :'shared-models' => {
      :label => ":scala: Subscriptions",
      :connectors => [:none]
    },
    :'images' => {
      :label => ":scala: Images",
      :connectors => [:all]
    },
    :'deploy' => {
      :label => ":scala: Deploy",
      :connectors => [:all]
    },
    :'api' => {
      :label => ":scala: API",
      :connectors => [:all]
    },
    :'workers' => {
      :label => ":scala: Workers",
      :connectors => [:all]
    }
  }

  @@wait_step = PipelineStep.new.wait!

  def initialize(context)
    @context = context
  end

  def render!
    steps = collect_steps
    rendered = <<~EOS
      steps:
      #{steps.compact.map { |step| step.render!(2) }.join "\n\n"}
    EOS

    puts rendered
    rendered
  end

  def collect_steps
    [ test_steps,
      build_steps,
      @@wait_step,
      release_artifacts_steps].flatten
  end

  def test_steps
    @@test_rules.map do |service, rule|
      rule[:connectors].map do |connector|
        case connector
        when :all
          @context.connectors.map do |registered_connector|
            PipelineStep.new
              .label("#{rule[:label]} [#{registered_connector}]")
              .command("./server/.buildkite/pipeline.sh test #{service} #{registered_connector}")
          end

        when :none
          PipelineStep.new
            .label(rule[:label])
            .command("./server/.buildkite/pipeline.sh test #{service}")

        else
          PipelineStep.new
            .label(rule[:connectors].length > 1 ? "#{rule[:label]} [#{connector}]" : rule[:label])
            .command("./server/.buildkite/pipeline.sh test #{service} #{connector == :none ? "" : connector}")
        end
      end
    end.flatten
  end

  def build_steps
    # @context.native_image_targets.map do |target|
    #   PipelineStep.new
    #     .label(":rust: Native image [#{target}]")
    #     .command("./server/build-cli/cli native-image #{target}")
    #     .queue("native-linux")
    # end

    [
      PipelineStep.new
        .label(":rust: Native image [debian]")
        .command("./server/.buildkite/scripts/native-image.sh")
        .queue("native-linux"),

      PipelineStep.new
        .label(":rust: Native image [lambda]")
        .command("./server/.buildkite/scripts/build-lambda.sh")
        .queue("native-linux")
    ]
  end

  def release_artifacts_steps
    # Option 1: It's a tag on master -> check branches match and build stable images for next tag.
    # Option 2: It's a tag, but on beta -> check branches match and add step to build beta image. todo: Useful?
    # Option 3: It's a normal build on either alpha or beta. Release images with incremented revision of the last tag on the channel.
    # Everything else doesn't trigger image builds

    steps = []

    if @context.tag != nil && @context.tag.stable? && @context.branch == "master"
      steps.push PipelineStep.new
        .label(":docker: Release stable #{@context.tag.stringify}")
        .command("./server/.buildkite/pipeline.sh build stable #{@context.tag.stringify}")

    elsif @context.tag != nil && !@context.tag.stable? && @context.branch == "beta"
      # Not sure this is super useful
      steps.push PipelineStep.new
        .label(":docker: Release beta #{@context.tag.stringify}")
        .command("./server/.buildkite/pipeline.sh build beta #{@context.tag.stringify}")

    elsif @context.branch == "alpha" || @context.branch == "beta"
      channel_tag = @context.last_git_tag.dup
      channel_tag.patch = nil # Ignore patches for unstable releases
      channel_tag.minor += (@context.branch == "alpha") ? 2 : 1
      last_docker_tag = @context.get_last_docker_tag_for("#{channel_tag.stringify}-#{@context.branch}-") # Check for previous revision of the unstable version

      if last_docker_tag.nil?
        next_docker_tag = "#{channel_tag.stringify}-#{@context.branch}-1"
      else
        next_docker_tag = "#{channel_tag.stringify}-#{@context.branch}-#{last_docker_tag.revision + 1}"
      end

      steps.push PipelineStep.new
        .label(":docker: Release #{@context.branch} #{next_docker_tag}")
        .command("./server/.buildkite/pipeline.sh build #{@context.branch} #{next_docker_tag}")
    end
  end
end
