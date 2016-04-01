# Sansible

A type-safe DSL to generate and run Ansible playbooks directly from within Scala.

## Motivation

With its agentless architecture and its rich collection of built-in modules,
Ansible provides a simple and effective solution for deploying software and provisioning
infrastructure.

However, as our Ansible code-base grew in both size and complexity, we found that managing a
large collection of playbooks in plain YAML was far from ideal. So we set out to explore the
possibility of using a statically typed language to generate and run our playbooks.
We found that this approach provided several major benefits:

- Increased safety and quicker developer feedback, expecially with respect to variable
  scoping and allowed module parameters.
- Full IDE Support, with autocompletion for module names and parameters.
- Superior means of abstraction than the ones offered by Ansible: no need to learn
  Ansible own loops syntax, variable scoping rules, jinja2 templates, includes, etc..
  Just use Scala!

## Development

Sansible heavily relies on a Scala macro to generate a collection of case classes
(with their respective serialisation logic) from the ansible core modules' sources.
The process involves a fair amount of data massaging (cloning git repositories,
parsing YAML module annotations, applying some overrides, etc.). Ansible module annotations
are in fact intended for documentation purposes and are not expressed in
a proper schema definition language.

## Usage

First build the library by running `sbt compile`. You will need a recent version
of Ruby installed, as we require it for pre-processing Ansible source annotations
(sadly we couldn't find any decent way to parse YAML in Scala). Once built, you can
publish the library in your local ivy2 cache by running `sbt ansible/publishLocal`.

At this point, should be able to use the library in a separate project.

```scala

package example.tasks

import ansible.Modules.{Apt, User}
import ansible.Task
import example.Conf.appName

object Dependencies {
   val appUser = Task(s"create user $appName",
    User(appName, state = Some(User.State.present))
  )

  val installGit = Task(
    "install git",
    Apt(name = Some("git"), state = Some(Apt.State.present)))

  ...

  val all = List(appUser, installGit)
}
```

Assuming we have defined an inventory and some tasks, we can now put them together in
a playbook and run it with ansible (we are currently developing against v2.0.1.0).

```scala

import ansible.Inventory.HostPattern
import ansible.Options.Become
import ansible.{Playbook, Runner}
import example.tasks._

object Example extends App {
  val playbook = Playbook(
    hosts = List(HostPattern(Inventory.Groups.web.name)),
    tasks = Dependencies.all ++ App.all
  )

  Runner.runPlaybook(Inventory.default)(playbook)
}

```

For a fully-working playbook example, please refer to the [sansible-examples](http://github.com/citycontext/sansible-examples) repo.

## Project status

The library is currently in its early development stage. We do not encourage
you using it in production just yet.
