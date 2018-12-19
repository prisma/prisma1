require_relative './pipeline'
require_relative './cmd'

def upload_pipeline(context)
  yml = PipelineRenderer.render!([
    # Locked connector steps
    PipelineStep.new
      .label(":mysql: MySql API connector")
      .command("./server/.buildkite/scripts/test.sh deploy-connector-mysql mysql"),

    PipelineStep.new
      .label(":mysql: MySql deploy connector")
      .command("./server/.buildkite/scripts/test.sh deploy-connector-mysql mysql"),

    PipelineStep.new
      .label(":mysql: integration-tests-mysql")
      .command("./server/.buildkite/scripts/test.sh deploy-connector-mysql mysql"),

    PipelineStep.new
      .label(":mysql: MySql deploy connector")
      .command("./server/.buildkite/scripts/test.sh integration-tests-mysql mysql"),

      PipelineStep.new
      .label(":postgres: Postgres API connector")
      .command("cd server && ./.buildkite/scripts/test.sh api-connector-postgres postgres"),

    PipelineStep.new
      .label(":postgres: Postgres deploy connector")
      .command("cd server && ./.buildkite/scripts/test.sh deploy-connector-postgres postgres"),

    PipelineStep.new
      .label(":postgres: integration-tests-postgres")
      .command("cd server && ./.buildkite/scripts/test.sh integration-tests-mysql postgres"),

    PipelineStep.new
      .label(":piedpiper: MongoDB API connector")
      .command("cd server && ./.buildkite/scripts/test.sh api-connector-mongo mongo"),

    PipelineStep.new
      .label(":piedpiper: MongoDB deploy connector")
      .command("cd server && ./.buildkite/scripts/test.sh deploy-connector-mongo mongo"),

    PipelineStep.new
      .label(":scala: libs")
      .command("cd server && ./.buildkite/scripts/test.sh libs mysql"),

    PipelineStep.new
      .label(":scala: subscriptions")
      .command("cd server && ./.buildkite/scripts/test.sh subscriptions postgres"),

    PipelineStep.new
      .label(":scala: shared-models")
      .command("cd server && ./.buildkite/scripts/test.sh shared-models mysql")#,

    # PipelineStep.new
    #   .label(":rust: Native image")
    #   .command("./server/.buildkite/scripts/native-image.sh"),
    #   .queue("native-linux")

    # PipelineStep.new
    #   .label(":lambda: Lambda native image")
    #   .command("./server/.buildkite/scripts/build-lambda.sh")
    #   .queue("native-linux")
  ], context)

  res = Command.new("buildkite-agent", "pipeline", "upload").with_stdin([yml]).run!
  if res.success?
    puts "Successfully uploaded pipeline"
  else
    puts <<~EOS
      Failed to upload pipeline: Exit #{res.status}
      Stdout: --------"
      #{res.print_stdout}

      Stderr: --------
      #{res.print_stderr}
    EOS
  end
end

# for connector in "${connectors[@]}"
# do
#  dynamic=$(printf "$dynamic
# %s
# " "    - label: \":scala: images [$connector]\"
#     command: cd server && ./.buildkite/scripts/test.sh images $connector

#   - label: \":scala: deploy [$connector]\"
#     command: cd server && ./.buildkite/scripts/test.sh deploy $connector

#   - label: \":scala: api [$connector]\"
#     command: cd server && ./.buildkite/scripts/test.sh api $connector

#   - label: \":scala: workers [$connector]\"
#     command: cd server && ./.buildkite/scripts/test.sh workers $connector

# ")
# done

# docker=$(printf "
#   - wait

#   - label: \":docker: Build alpha channel\"
#     command: ./server/.buildkite/scripts/unstable.sh alpha 2
#     branches: alpha

#   - label: \":docker: Check & Build beta channel\"
#     command: ./server/.buildkite/scripts/unstable.sh beta 1

#   - label: \":docker: Check & Build stable channel\"
#     command: ./server/.buildkite/scripts/stable.sh
#   ")

# pipeline=$(printf "
# steps:
# $static
# $dynamic
# $docker
# ")

# echo "$pipeline" | buildkite-agent pipeline upload
