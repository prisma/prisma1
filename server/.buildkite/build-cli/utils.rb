require 'open3'

class ShellResult
    def initialize(status, std_out, std_err)
        @status = status
        @std_out = std_out
        @std_err = std_err
    end
end

def run(cmd, args)
    stdout, stderr, status = Open3.capture3(cmd, args)
end