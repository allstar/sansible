#! /usr/bin/env ruby
require 'yaml'
require 'json'
require 'fileutils'

SRC_DIR       = ARGV[0]
TARGET_DIR    = ARGV[1]
YAMLDOC_START = /^DOCUMENTATION\s*=\s*("""|r?''')\n$/
YAMLDOC_END   = /^('''|""")/

unless File.directory?(SRC_DIR)
  $stderr.puts "#{SRC_DIR} is not a directory"
end

unless File.directory?(TARGET_DIR)
  $stderr.puts "#{TARGET_DIR} is not a directory"
end

Dir.glob("#{SRC_DIR}/**/*.py").reject { |path| path.end_with?("__init__.py") }.each do |path|
  module_name = File.basename(path, '.py')
  package = File.dirname(path).split("/").last
  lines = File.readlines(path)
  is_module = lines.any? { |l| l =~ YAMLDOC_START }

  if is_module
    yaml =
      lines
      .drop_while { |l| !(l =~ YAMLDOC_START)}
      .take_while { |l| !(l =~ YAMLDOC_END) }
      .drop(1)
      .join("\n")

    begin
      module_def = YAML.load(yaml)
      if module_def.respond_to?(:merge!)
        module_def.merge!(package: package) 
        dir = File.join(TARGET_DIR, package)
        FileUtils.mkdir_p(dir)
        File.open(File.join(dir, "#{module_name}.json"), 'w') { |f| f.write(JSON.pretty_generate(module_def)) }
      else
        $stderr.puts "Extracted malformed YAML for module #{path}"
      end
    rescue Psych::SyntaxError
      $stderr.puts "Failed parsing yaml for module #{module_name}, path: #{path}"
    end
  end
end
