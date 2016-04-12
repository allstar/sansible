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

- Increased safety and quicker developer feedback, especially with respect to variable
  scoping and allowed module parameters.
- Full IDE Support, with auto-completion for module names and parameters.
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
import example.Conf.appUsername

object Dependencies {
   val appUser = Task(s"create user $appUsername",
    User(appUsername).withState(User.State.present)
  )

  val installGit = Task("install git",
    Apt(name = Some("git")).withState(Apt.State.latest)
  )

  val all = List(appUser, installGit)
}
```
Assuming we have defined an inventory and some tasks, we can now put them together in
a playbook and run it with ansible (we are currently developing against v2.0.1.0).

```scala

import ansible.Inventory.HostPattern
import ansible.{Playbook, Runner}
import example.tasks._

object Example extends App {
  val playbook = Playbook(
    hosts = List(HostPattern(Inventory.Groups.web.name)),
    tasks = Dependencies.all
  )
  Runner.runPlaybook(Inventory.default)(playbook)
}
```

For some fully working playbook examples, please refer to the [sansible-examples](http://github.com/citycontext/sansible-examples) repo on Github.

### Generated module code

The library provides auto-generated case classes for each Ansible module defined in the [ansible-modules-core](https://github.com/ansible/ansible-modules-core) repository.
Additionally, we generate a sealed sum type for each enumerable module option. For instance, for the [service](http://docs.ansible.com/ansible/service_module.html) module's **state** option,
we generate the following Scala code

```scala

object Service {
  sealed trait State

  object State {
    case object Started extends State
    case object Stopped extends State
    case object Restarted extends State
    case object Reloaded extends State

    val started: State = Started
    val stopped: State = Stopped
    val restarted: State = Stopped
    val reloaded: State = Reloaded
  }
}
```
A service task, can now be defined by calling Service's `apply` method with the desired arguments:

```scala
val esRestart = Task("restart elasticsearch", Service("elasticsearch",
  state = Some(Service.State.restarted),
)
```
Notice that, unless a module argument is explicitly marked as required in the Ansible documentation,
we have to represent it in Sansible as a Scala `Option`. In order to mitigate the additional boilerplate
that this introduces, we provide an auto-generated setter method for each non-mandatory enumerable field.
This allows to re-write the service module call above as follows:

```scala
Service("elasticsearch").withState(Service.State.restarted)
```

### Common options DSL

A number of Ansible options are equally applicable to both tasks and playbooks.
These includes, among others, things like tags, environment variables, serial execution,
and privilege escalation. In order to use the DSL, you will need to import implicits from both
`ansible.std._` and `ansible.dsl._`.

```scala
import ansible.{Task, Playbook}
import ansible.Modules.GetUrl
import ansible.std._
import ansible.dsl._

val downloadApp = Task("Download app jar", GetUrl(
  url = "http://example.com/jars/app.jar",
  dest = "/home/appUser/app"
)).becoming("appUser").withTags("tag1", "tag2")

```
For an overview of the methods currently implemented, refer to the implicit class
`ansible.dsl.CommonOptions.Syntax`, which will be in scope for both playbook and
task objects.

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
