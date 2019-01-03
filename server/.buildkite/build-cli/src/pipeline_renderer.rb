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
      #{steps.map { |step| step.render!(2) }.join "\n\n"}
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
              # .command("cd server && ./.buildkite/scripts/test.sh #{service} #{registered_connector}")

          end

        when :none
          PipelineStep.new
            .label(rule[:label])
            .command("./server/.buildkite/pipeline.sh test #{service}")
            # .command("cd server && ./.buildkite/scripts/test.sh #{service} mysql")

        else
          PipelineStep.new
            .label(rule[:connectors].length > 1 ? "#{rule[:label]} [#{connector}]" : rule[:label])
            .command("./server/.buildkite/pipeline.sh test #{service} #{connector == :none ? "" : connector}")
            # .command("cd server && ./.buildkite/scripts/test.sh #{service} #{connector == :none ? "" : connector}")
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
    [
      PipelineStep.new
        .label(":docker: Build alpha channel")
        .command("./server/.buildkite/scripts/unstable.sh alpha 2")
        .branches([:alpha]),

      PipelineStep.new
        .label(":docker: Check & Build beta channel")
        .command("./server/.buildkite/scripts/unstable.sh beta 1"),

      PipelineStep.new
        .label(":docker: Check & Build stable channel")
        .command("./server/.buildkite/scripts/stable.sh")
    ]
  end
end
