# Sansible
[![Maven central](https://maven-badges.herokuapp.com/maven-central/com.citycontext/sansible_2.11/badge.png?style=plastic)](https://maven-badges.herokuapp.com/maven-central/com.citycontext/sansible_2.11)
[![Build Status](https://travis-ci.org/citycontext/sansible.png)](https://travis-ci.org/citycontext/sansible)

A type-safe DSL to generate and run Ansible playbooks directly from within Scala.

## Motivation

With its agentless architecture and its rich collection of built-in modules,
Ansible provides a simple and effective solution for deploying software and provisioning
infrastructure. However, as our Ansible code-base grew, we found that managing a
large collection of playbooks in plain YAML was far from ideal. Instead, using Ansible as a
DSL embedded in Scala has the following benefits:

- Increased safety and quicker developer feedback, expecially with respect to variable
  scoping and allowed module parameters.
- Full IDE Support, with autocompletion for module names and parameters.
- Superior means of abstraction: no need to learn Ansible own control flow syntax, variable scoping rules, jinja2 templates, includes, etc..
  Just use Scala!

## Usage

Add Sansible to your `build.sbt`:

```scala
libraryDependencies += "com.citycontext" %% "sansible" % version
```

Break your provisioning code into packages and modules, exactly as you do for your
scala projects.

```scala

package example.tasks

import ansible.Modules.{Apt, User}
import ansible.Task
import ansible.std._
import ansible.dsl._
import example.Conf.appUsername

object Dependencies {
   val appUser = Task(s"create user $appName",
    User(appUsername).withState(User.State.present)
  )

  val installGit = Task(
    "install git",
    Apt(name = Some("git")).withState(Apt.State.latest)
  )

  val all = List(appUser, installGit).map(_.withTags("dependencies", "apt"))
}
```

Assuming we have defined an inventory and some tasks, we can now put them together in
a playbook and run it with ansible (we are currently developing against v2.0.1.0).

```scala

import ansible.Inventory.HostPattern
import ansible.{Playbook, Runner}
import ansible.std._
import ansible.dsl._
import example.tasks._

object Example extends App {
  val playbook = Playbook(
    hosts = List(HostPattern(Inventory.Groups.web.name)),
    tasks = Dependencies.all ++ App.all
  ).withEnv(Map(
     "http_proxy" -> "http://proxy.example.com:8080"
  ))

  Runner.runPlaybook(Inventory.default)(playbook)
}

```

For fully-working playbook examples, please refer to the [sansible-examples](http://github.com/citycontext/sansible-examples) repo.

## Development

Sansible relies on a Ruby script and a Scala macro to generate a collection of case classes
and their respective serialisation logic from the Ansible core modules' sources.
The process involves a fair amount of data massaging, such as cloning git repositories,
parsing YAML module annotations, applying some overrides, etc. Ansible module annotations
are in fact intended for documentation purposes and are not expressed in
a proper schema definition language.

## Project status

The library is currently in its early development stage. We do not encourage
you using it in production just yet.
