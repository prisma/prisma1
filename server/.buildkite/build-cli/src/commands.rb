require_relative './pipeline_renderer'
require_relative './command'
require_relative './docker'

def upload_pipeline(context)
  yml = PipelineRenderer.new(context).render!
  res = Command.new("buildkite-agent", "pipeline", "upload").with_stdin([yml]).run!.raise!
  
  if res.success?
    puts "Successfully uploaded pipeline"
  end
end

def test_project(context, project, connector)
  DockerCommands.kill_all
  DockerCommands.run_tests_for(context, project, connector)
end
