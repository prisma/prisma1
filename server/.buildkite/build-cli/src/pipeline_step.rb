class PipelineStep
  def initialize
    @label = ""
    @command = ""
    @queue = nil
    @branches = []
    @wait = false
  end

  def wait!
    @wait = true
    self
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
    if @wait
      return " " * indent + "- wait"
    end

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
        "    queue: #{@queue}"
      ]
    end

    rendered.map { |render| " " * indent + render }.join("\n")
  end
end