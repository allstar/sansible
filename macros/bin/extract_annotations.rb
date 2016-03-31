#! /usr/bin/env ruby

require 'yaml'
require 'json'
require 'fileutils'

SRC_DIR                = ARGV[0]
OVERRIDES_DIR          = ARGV[1]
TARGET_DIR             = ARGV[2]
YAMLDOC_START          = /^\s*DOCUMENTATION\s*=\s*("""|r?''')$/
YAMLDOC_END            = /^('''|""")/
OVERRIDES              = {}

[SRC_DIR, OVERRIDES_DIR, TARGET_DIR].each do |dir|
  unless dir && File.directory?(dir)
    $stderr.puts "usage: #{$0} SRC_DIR OVERRIDES_DIR TARGET_DIR"
    exit 1
  end
end

def parse_doc(path)
  lines = File.readlines(path)
  is_module = lines.any? { |l| l =~ YAMLDOC_START }

  return {} unless is_module

  yaml =
    lines
    .drop_while { |l| !(l =~ YAMLDOC_START)}
    .take_while { |l| !(l =~ YAMLDOC_END) }
    .drop(1)
    .join("\n")

  begin
    YAML.load(yaml)
  rescue Psych::SyntaxError
    $stderr.puts "Failed parsing yaml from file #{path}"
    {}
  end
end

def doc_overrides(module_name)
  case module_name
  when 'assemble', 'file', 'blockinfile', 'lineinfile', 'copy', 'replace', 'template'
    OVERRIDES['backup'].merge(OVERRIDES['files']).merge(OVERRIDES['validate'])
  when 'synchronize', 'inifile', 'copy', 'unarchive'
    OVERRIDES['files']
  when 'mysql_db', 'mysql_replication', 'mysql_user', 'mysql_variables'
    OVERRIDES['mysql']
  else
    {}
  end
end

Dir.glob("#{OVERRIDES_DIR}/**/*.py").each do |path|
  package = File.basename(path, '.py')
  doc = parse_doc(path).fetch('options', {})
  OVERRIDES.merge!(package => doc) unless doc.empty?
end

Dir.glob("#{SRC_DIR}/**/*.py").reject { |path| path.end_with?("__init__.py") }.each do |path|
  module_name = File.basename(path, '.py')
  package = File.dirname(path).split("/").last

  begin
    module_def = parse_doc(path)
    unless module_def.empty?
      module_def.merge!('package' => package)
      module_def.fetch('options', {}).merge!(doc_overrides(module_name))
      dir = File.join(TARGET_DIR, package)
      FileUtils.mkdir_p(dir)
      File.open(File.join(dir, "#{module_name}.json"), 'w') { |f| f.write(JSON.pretty_generate(module_def)) }
    else
      $stderr.puts "Extracted malformed YAML for module #{path}"
    end
  end
end
