require_relative './pipeline_renderer'
require_relative './cmd'

def upload_pipeline(context)
  yml = PipelineRenderer.new(context).render!
  res = Command.new("buildkite-agent", "pipeline", "upload").with_stdin([yml]).run!
  if res.success?
    puts "Successfully uploaded pipeline"
  else
    wat =<<~EOS
      Failed to upload pipeline: Exit #{res.status}
      Stdout: --------
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
