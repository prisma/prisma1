require 'rbconfig'
require 'pathname'

class BuildContext
  attr_accessor :branch, :tag, :last_git_tag, :next_docker_tags, :server_root_path

  def initialize
    @branch = ENV["BUILDKITE_BRANCH"] || "local"
    @tag = ENV["BUILDKITE_TAG"] || nil
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
    server_changed? || local_build? || tagged?
  end
  
  def tagged?
    @tag != nil
  end

  def server_changed?
    # if [ -z "$BUILDKITE_TAG" ]; then
    #   # Regular commit, not a tag
    #   git diff --exit-code --name-only ${BUILDKITE_COMMIT} ${BUILDKITE_COMMIT}~1 | grep "server/"
    #   if [ $? -ne 0 ]; then
    #       echo "Nothing to do"
    #       exit 0
    #   fi
    # fi

  end
  
  def local_build?
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