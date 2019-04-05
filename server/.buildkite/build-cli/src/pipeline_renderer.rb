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
    release_steps = release_artifacts_steps
    [ test_steps,
      rust_tests,
      release_steps[:before_wait],
      @@wait_step,
      release_rust_artifacts,
      release_steps[:after_wait]].flatten
  end

  def test_steps
    []
    # @@test_rules.map do |service, rule|
    #   rule[:connectors].map do |connector|
    #     case connector
    #     when :all
    #       @context.connectors.map do |registered_connector|
    #         PipelineStep.new
    #           .label("#{rule[:label]} [#{registered_connector}]")
    #           .command("./server/.buildkite/pipeline.sh test #{service} #{registered_connector}")
    #       end

    #     when :none
    #       PipelineStep.new
    #         .label(rule[:label])
    #         .command("./server/.buildkite/pipeline.sh test #{service}")

    #     else
    #       PipelineStep.new
    #         .label(rule[:connectors].length > 1 ? "#{rule[:label]} [#{connector}]" : rule[:label])
    #         .command("./server/.buildkite/pipeline.sh test #{service} #{connector == :none ? "" : connector}")
    #     end
    #   end
    # end.flatten
  end

  def rust_tests
      PipelineStep.new
        .label(":rust: Cargo test prisma-rs")
        .command("./server/.buildkite/pipeline.sh test-rust")
  end

  def release_rust_artifacts
    [
      PipelineStep.new
        .label(":rust: Build & Publish :linux:")
        .command("./server/.buildkite/pipeline.sh rust-binary"),
      PipelineStep.new
        .label(":rust: Build & Publish :darwin:")
        .command("./server/.buildkite/pipeline.sh rust-binary")
        .queue("macos")
    ]
  end

  def release_artifacts_steps
    # Option 1: It's a tag on master -> check branches match and build stable images for next tag.
    # Option 2: It's a tag on beta -> check branches match and build beta image.
    # Option 3: It's a tag, but on beta -> check branches match and add step to build beta image. todo: Useful?
    # Option 4: It's a normal build on either alpha or beta. Release images with incremented revision of the last tag on the channel.
    # Everything else doesn't trigger image builds, only build native images for compile-checks

    steps = {
      :before_wait => [],
      :after_wait => []
    }

    if @context.tag != nil && @context.tag.stable? && @context.branch == @context.tag.stringify
      # steps[:before_wait] = build_steps_for(@context.tag.stringify)
      steps[:after_wait].push PipelineStep.new
        .label(":docker: Release stable #{@context.tag.stringify}")
        .command("./server/.buildkite/pipeline.sh build #{@context.tag.stringify}")

    elsif @context.tag != nil && @context.tag.beta? && @context.branch == @context.tag.stringify
      next_tag = @context.branch
      # steps[:before_wait] = build_steps_for(next_tag)
      steps[:after_wait].push PipelineStep.new
        .label(":docker: Release #{@context.branch} #{next_tag}")
        .command("./server/.buildkite/pipeline.sh build #{next_tag}")

    elsif @context.branch == "alpha" || @context.branch == "beta"
      next_tag = calculate_next_unstable_docker_tag()
      # steps[:before_wait] = build_steps_for(next_tag.stringify)
      steps[:after_wait].push PipelineStep.new
        .label(":docker: Release #{@context.branch} #{next_tag.stringify}")
        .command("./server/.buildkite/pipeline.sh build #{next_tag.stringify}")

    else
      # steps[:before_wait] = build_steps_for(@context.branch)
    end

    steps
  end

  def build_steps_for(version)
    # ['debian', 'lambda'].map do |target|
    #   PipelineStep.new
    #     .label(":rust: Native image [#{target}] [#{version}]")
    #     .command("./server/.buildkite/pipeline.sh native-image #{target} #{version}")
    #     .queue("native-linux")
    # end
    []
  end

  def calculate_next_unstable_docker_tag()
    @next_docker_tag ||= lambda do
      new_tag = @context.last_git_tag.dup
      new_tag.channel = @context.branch
      new_tag.patch = nil  # Ignore patches for unstable releases
      new_tag.minor += (@context.branch == "alpha") ? 2 : 1
      last_docker_tag = @context.get_last_docker_tag_for("#{new_tag.stringify}-") # Check for previous revision of the unstable version

      if last_docker_tag.nil?
        new_tag.revision = 1
      else
        new_tag.revision = last_docker_tag.revision + 1
      end

      new_tag
    end.call

    @next_docker_tag
  end
end
