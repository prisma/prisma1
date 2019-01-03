require_relative './command'

class DockerCommands
  def self.kill_all
    puts "Stopping all docker containers..."
    containers = Command.new("docker", "ps", "-q").run!.raise!
    containers.stdout.each do |container|
      puts "\tStopping #{container.chomp}..."
      Command.new("docker", "kill", container.chomp).run!.raise!
    end
  end

  def self.run_tests_for(context, project, connector)
    compose_flags = ["--project-name", "#{project}", "--file", "#{context.server_root_path}/.buildkite/scripts/docker-test-setups/docker-compose.test.#{connector}.yml"]
    
    # Start rabbit and db, wait a bit for init
    puts "Starting dependency services..."
    deps = Command.new("docker-compose", *compose_flags, "up", "-d", "test-db", "rabbit").puts!.run!.raise!
    
    sleep(15)

    puts "Starting tests for #{project}..."
    test_run = Command.new("docker-compose", *compose_flags, "run", "app", "sbt", "-mem", "3072", "#{project}/test").puts!.run!.raise!

    puts "Stopping dependency services..."
    cleanup = Command.new("docker-compose", *compose_flags, "kill").puts!.run!.raise!

    # Only the test run result is important
    test_run
  end
end