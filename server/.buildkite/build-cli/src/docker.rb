require_relative './command'

class DockerCommands
  @images = ["prisma", "prisma-prod"]

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

  def self.build(context, prisma_version)
    Command.new("docker", "run", "-e", "BRANCH=#{context.branch}", "-e", "COMMIT_SHA=#{context.commit}", "-e", "CLUSTER_VERSION=#{prisma_version.stringify}",
      '-w', '/root/build',
      '-v', "#{context.server_root_path}:/root/build",
      '-v', "#{File.expand_path('~')}/.ivy2:/root/.ivy2",
      '-v', "#{File.expand_path('~')}/.coursier:/root/.coursier",
      '-v', '/var/run/docker.sock:/var/run/docker.sock',
      'prismagraphql/build-image:debian',
      'sbt', 'docker').puts!.run!.raise!
  end

  def self.tag_and_push(tags)
    tags.each do |tag|
      puts "Tagging prismagraphql/$service:latest image with $DOCKER_TAG..."
  # docker tag prismagraphql/${service}:latest prismagraphql/${service}:${DOCKER_TAG}
  # docker tag prismagraphql/${service}:graalvm prismagraphql/${service}:graalvm-${DOCKER_TAG}
    end

  puts "Pushing prismagraphql/$service:$DOCKER_TAG..."
  # docker push prismagraphql/${service}:${DOCKER_TAG}
  # docker push prismagraphql/${service}:graalvm-${DOCKER_TAG}
  end
end