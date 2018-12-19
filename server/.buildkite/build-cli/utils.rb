require 'open3'

class ExecResult
    attr_accessor :status, :stdout, :stderr

    def initialize(status, stdout, stderr)
        @status = status
        @stdout = stdout
        @stderr = stderr
    end
end

def run(cmd, args)
    stdout, stderr, status = Open3.capture3(cmd, args)
    ExecResult.new(status, stdout, stderr)
end