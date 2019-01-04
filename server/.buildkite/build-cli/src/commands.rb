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

def build_images(context, tag)
  DockerCommands.kill_all

  tags_to_build = [tag.stringify]
  if tag.stable?
    if tag.patch.nil?
      # E.g. not only tag 1.30, but also 1.30.0
      additional_tag = tag.dup
      additional_tag.patch = 0
      tags_to_build.push additional_tag.stringify
    else
      # E.g. not only tag 1.30.0, but also 1.30
      additional_tag = tag.dup
      additional_tag.patch = nil
      tags_to_build.push additional_tag.stringify
    end
  else
    if tag.revision.nil?
      # E.g. not only tag 1.30-beta, but also 1.30-beta-1
      additional_tag = tag.dup
      additional_tag.revision = 1
      tags_to_build.push additional_tag.stringify
    else
      # E.g. not only tag 1.30-beta-1, but also 1.30-beta
      additional_tag = tag.dup
      additional_tag.revision = nil
      tags_to_build.push additional_tag.stringify
    end
  end

  DockerCommands.build(context, tag)
  DockerCommands.tag_and_push(context, tags_to_build)

  trigger_dependent_pipeline(context.branch, tags_to_build)
end


def trigger_dependent_pipeline(channel, tags)
  pipeline_input = <<~EOS
    - trigger: \"prisma-cloud\"
      label: \":cloud: Trigger Prisma Cloud Tasks #{tags.join(", ")} :cloud:\"
      async: true
      build:
        env:
            BUILD_TAGS: \"#{tags.join(',')}\"
            CHANNEL: \"#{channel}\"
  EOS

  puts pipeline_input

  res = Command.new("buildkite-agent", "pipeline", "upload").with_stdin([pipeline_input]).run!.raise!
end