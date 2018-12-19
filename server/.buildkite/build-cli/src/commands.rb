require_relative './pipeline'
require_relative './cmd'

def upload_pipeline(context)
  yml = PipelineRenderer.render!([
    PipelineStep.new
      .label(":mysql: MySql API connector")
      .command("./server/.buildkite/scripts/test.sh deploy-connector-mysql mysql"),

    PipelineStep.new
      .label(":mysql: MySql deploy connector")
      .command("./server/.buildkite/scripts/test.sh deploy-connector-mysql mysql")
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
