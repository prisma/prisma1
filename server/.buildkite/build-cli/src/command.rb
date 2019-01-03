require 'open3'

class Command
  attr_accessor :stdin, :cmd, :args, :stream_output, :pipe_to

  def initialize(command, *arguments)
    @cmd = command
    @args = arguments
    @stream_output = false
  end

  ## input: array of strings
  def with_stdin(input)
    self.stdin = input
    self
  end

  def puts!
    @stream_output = true
    self
  end

  def pipe_stdout_to(other_command)
    self.pipe_to = other_command
    self
  end

  def run!
    outputs = { :stdout => [], :stderr => [] }

    begin
      Open3.popen3(cmd, *args) do |s_in, stdout, stderr, thread|
        unless stdin.nil?
          stdin.each { |line| s_in.puts line }
          s_in.close
        end

        { :stdout => stdout, :stderr => stderr }.each do |stream_key, stream|
          Thread.new do
            while line = stream.gets do
              outputs[stream_key].push line
              puts "[#{stream_key}]: #{line}" if stream_output
            end
          end
        end

        thread.join
        exit_status = thread.value
        result = ExecResult.new(exit_status, outputs[:stdout], outputs[:stderr])

        if pipe_to != nil && result.success?
          pipe_to.with_stdin(result.stdout).run!
        else
          ExecResult.new(exit_status, outputs[:stdout], outputs[:stderr])
        end
      end
    rescue => e
      ExecResult.new(-1, ["No such command, file, or directory: #{cmd}"], [])
    end
  end
end

class ExecResult
  attr_accessor :status, :stdout, :stderr

  def initialize(status, stdout, stderr)
    @status = status
    @stdout = stdout
    @stderr = stderr
  end

  def get_stdout
    stdout.join('')
  end

  def get_stderr
    stderr.join('')
  end

  def print_stdout
    puts get_stdout
  end

  def print_stderr
    puts get_stderr
  end

  def success?
    status == 0
  end
end