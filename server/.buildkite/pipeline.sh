#!/usr/bin/env ruby -w
## Retained for backwards compatibility until merged into all branches.

# Regardless of where the script is called, always use the script location as base.
Dir.chdir(__dir__)

require_relative './build-cli/cli'

CLI.main(ARGV)