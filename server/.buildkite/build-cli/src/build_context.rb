require 'rbconfig'
require 'pathname'
require 'uri'
require 'net/http'
require 'json'
require_relative './command'

class BuildContext
  attr_accessor :branch, :tag, :commit, :last_git_tag, :server_root_path

  def initialize
    @branch = ENV["BUILDKITE_BRANCH"] || nil
    @tag = (ENV["BUILDKITE_TAG"].nil? || ENV["BUILDKITE_TAG"].empty?) ? nil : Tag.new(ENV["BUILDKITE_TAG"])
    @commit = ENV["BUILDKITE_COMMIT"] || nil
    @last_git_tag = get_last_git_tag
    @server_root_path = find_server_root
  end

  def get_last_git_tag
    last_tags = Command.new("git", "tag", "--sort=-version:refname").run!.raise!
    puts last_tags.get_stdout
    filtered = last_tags.get_stdout.lines.map(&:chomp).select { |tag| !tag.include?("beta") && !tag.start_with?("v") }
    puts "Last tag: #{filtered.first}"
    Tag.new(filtered.first)


    # last_tag = Command.new("git", "tag", "--sort=-version:refname").pipe_stdout_to(
    #   Command.new("grep", "-v", "-e", "v", "-e", "beta").puts!.pipe_stdout_to(
    #     Command.new("head", "-n", "1").puts!
    #   )
    # ).run!.raise!

    # Tag.new(last_tag.get_stdout.chomp)
  end

  def get_last_docker_tag_for(tag_prefix)
    uri = URI.parse("https://registry.hub.docker.com/v2/repositories/prismagraphql/prisma/tags/")
    tags = []
    depth = 3 # Pagination depth max to retrieve tags

    loop do
      response = Net::HTTP.get_response(uri)
      code = response.code.to_i

      if code >= 200 && code < 300
        tags_json = JSON.parse(response.body)

        if tags_json['next'] != nil && depth > 0
          depth -= 1
          uri = URI.parse(tags_json['next'])
          tags_json['results'].each do |tag|
            tags.push tag['name']
          end
        else
          break
        end

      elsif code == 301
        uri = URI.parse(response.header['location'])

      else
        response.each_header do |key, value|
          p "#{key} => #{value}"
        end
        raise "Failed to fetch docker tags for Prisma: #{response.code} #{response.body}"
      end
    end

    tag = tags.find do |tag|
      tag != "latest" && !tag.include?("heroku") && tag.include?(tag_prefix)
    end

    if tag != nil
      Tag.new(tag)
    else
      nil
    end
  end

  def cli_invocation_path
    "#{server_root_path}/.buildkite/pipeline.sh"
  end

  def is_windows?
    os == :windows
  end

  def is_nix?
    os == :macosx || os == :unix || os == :linux
  end

  def should_build?
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
    @branch != "local"
  end

  def connectors
    [:mongo, :postgres, :mysql]
  end

  def native_image_targets
    [:debian, :lambda]
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

class Tag
  attr_accessor :major, :minor, :patch, :channel, :revision

  def initialize(tag)
    unless tag.nil? || !tag.include?(".")
      chunked = tag.split("-")
      raw_version = chunked[0]

      if chunked.length > 2
        @channel = chunked[1]
      end

      if chunked.length == 3
        @revision = chunked[2].to_i
      end

      @major, @minor, @patch = raw_version.split(".").map { |x| x.to_i }
    end
  end

  def nil?
    @major.nil? || @minor.nil?
  end

  def stable?
    !nil? && @channel.nil?
  end

  def stringify
    if nil?
      ""
    else
      stringified = "#{@major}.#{@minor}#{@patch.nil? ? "" : ".#{@patch}"}"
      unless @channel.nil?
        stringified += "-#{@channel}"
      end

      unless @revision.nil?
        stringified += "-#{@revision}"
      end

      stringified
    end
  end
end