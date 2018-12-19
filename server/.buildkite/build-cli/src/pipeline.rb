class PipelineRenderer
  @@rules = {
    :'api-connector-mysql' => {
      :label => ":mysql: MySql API connector"
      :connectors => [:mysql]
    },
    :'deploy-connector-mysql' => {
      :label => ":mysql: MySql deploy connector"
      :connectors => [:mysql]
    },
    :'integration-tests-mysql' => {
      :label => ":mysql: MySql integration tests"
      :connectors => [:mysql]
    },
    :'api-connector-postgres' => {
      :label => ":postgres: Postgres API connector"
      :connectors => [:postgres]
    },
    :'deploy-connector-postgres' => {
      :label => ":postgres: Postgres deploy connector"
      :connectors => [:postgres]
    }
  }

  def self.render!(context)
    rendered = <<~EOS
      steps:
      #{steps.map { |step| step.render!(2) }.join "\n\n"}
    EOS

    puts rendered

    rendered
  end
end

class PipelineStep
  def initialize
    @label = ""
    @command = ""
    @queue = nil
    @branches = []
  end

  def label(l)
    @label = l
    self
  end

  def command(c)
    @command = c
    self
  end

  def queue(q)
    @queue = q
    self
  end

  def branches(b)
    @branches = b
    self
  end

  def render!(indent = 2)
    rendered = [
      "- label: \"#{@label}\"",
      "  command: #{@command}"
    ]

    unless @branches.empty?
      rendered.push "  branches: #{@branches.join(' ')}"
    end

    unless @queue.nil?
      rendered += [
        "  agents:",
        "    queue: #{queue}"
      ]
    end

    rendered.map { |render| " " * indent + render }.join("\n")
  end
end