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
    compose_flags = ["--project-name", "#{project}", "--file", "#{context.server_root_path}/.buildkite/docker-test-setups/docker-compose.test.#{connector}.yml"]

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

  def self.tag_and_push(context, tags)
    # On the stable channel we additionally release -graalvm versions, running on graalvm.
    from_tags = context.branch == 'master' ? ['latest', 'graalvm'] : ['latest']

    @images.product(from_tags, tags).each do |image, from_tag, to_tag|
      suffix = from_tag == 'graalvm' ? '-graalvm' : ''

      puts "Tagging and pushing prismagraphql/#{image}:#{from_tag} image with #{to_tag}#{suffix}..."
      Command.new("docker", "tag", "prismagraphql/#{image}:#{from_tag}", "prismagraphql/#{image}:#{to_tag}#{suffix}").puts!.run!.raise!
      Command.new("docker", "push", "prismagraphql/#{image}:#{to_tag}#{suffix}").puts!.run!.raise!
    end
  end

  def self.native_image(context, prisma_version, build_image)
    Command.new("docker", "run", "-e", "BRANCH=#{context.branch}", "-e", "COMMIT_SHA=#{context.commit}", "-e", "CLUSTER_VERSION=#{prisma_version}",
      '-w', '/root/build',
      '-v', "#{context.server_root_path}:/root/build",
      '-v', "#{File.expand_path('~')}/.ivy2:/root/.ivy2",
      '-v', "#{File.expand_path('~')}/.coursier:/root/.coursier",
      '-v', '/var/run/docker.sock:/var/run/docker.sock',
      "prismagraphql/#{build_image}",
      'sbt', 'project prisma-native', "prisma-native-image:packageBin").puts!.run!.raise!
  end
end