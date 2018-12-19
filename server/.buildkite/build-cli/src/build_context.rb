require 'rbconfig'
require 'pathname'
require_relative './cmd'

class BuildContext
  attr_accessor :branch, :tag, :commit, :last_git_tag, :next_docker_tags, :server_root_path, :connectors

  @connectors = [:mongo, :postgres, :mysql]

  def initialize
    @branch = ENV["BUILDKITE_BRANCH"] || nil
    @tag = ENV["BUILDKITE_TAG"] || nil
    @commit = ENV["BUILDKITE_COMMIT"] || nil
    @last_git_tag = nil
    @next_docker_tags = []
    @server_root_path = find_server_root
  end

  def is_windows?
    os == :windows
  end

  def is_nix?
    os == :macosx || os == :unix || os == :linux
  end

  def valid_build?
    (server_changed? || !tag != nil) && buildkite_build?
  end

  def server_changed?
    if commit.nil?
      false
    else
      res = Command.new("git", "diff", "--exit-code", "--name-only", "#{commit}", "#{commit}~1").pipe_stdout_to(
        Command.new("grep", '"server"')
      ).run!
      res.status == 0
    end
  end

  def buildkite_build?
    @branch == "local"
  end

  private

  def os
    @os ||= (
      host_os = RbConfig::CONFIG['host_os']
      case host_os
      when /mswin|msys|mingw|cygwin|bccwin|wince|emc/
        :windows
      when /darwin|mac os/
        :macosx
      when /linux/
        :linux
      when /solaris|bsd/
        :unix
      else
        raise "Unknown host system: #{host_os.inspect}"
      end
    )
  end


  def find_server_root
    Pathname.new(Dir.pwd).parent.dirname
  end
end