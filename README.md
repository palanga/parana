Paraná
======

[![CI][Badge-CI]][Link-CI]
[![Release Artifacts][Badge-SonatypeReleases]][Link-SonatypeReleases]
[![Snapshot Artifacts][Badge-SonatypeSnapshots]][Link-SonatypeSnapshots]

An event sourcing library on top of ZIO
---------------------------------------

Installation
------------

We publish to maven central so you just have to add this to your `build.sbt` file:

```sbt
libraryDependencies += "dev.palanga" %% "parana-core-local" % "version"
```

[//]: # (We have a journal implementation with zio-cassandra and the same journal with json codec using zio-json.)

[//]: # (So you can use one of both:)

[//]: # ()
[//]: # (```sbt)

[//]: # (libraryDependencies += "dev.palanga" %% "parana-journal-cassandra"      % "version")

[//]: # (libraryDependencies += "dev.palanga" %% "parana-journal-cassandra-json" % "version")

[//]: # (```)

To get snapshot releases:

```sbt
resolvers += "Sonatype OSS Snapshots" at "https://s01.oss.sonatype.org/content/repositories/snapshots",
```

A tease:
--------

```scala
package palanga.examples.groups

import model.*
import commands.*
import events.*
import eventsourcing.*
import palanga.parana.eventsource.*
import palanga.parana.eventsource.local.*
import palanga.parana.journal.*
import zio.*

object app extends ZIOAppDefault:

  override def run = app.provide(groupsLocal, inMemoryJournal)

  private val app =
    for
      groups       <- ZIO.service[EventSource[Group, Command, Event]]
      ((id, _), _) <- groups.empty.ask(Command.Join(User("Palan")))
      _            <- groups.of(id).ask(Command.Join(User("Nube")))
      _            <- groups.of(id).ask(Command.Leave(User("Palan")))
      _            <- groups.of(id).ask(Command.Join(User("Fruchi")))
      _            <- groups.of(id).get
    yield ()

object model:

  case class Group(members: Set[User]):
    def join(user: User): Group  = copy(members = members + user)
    def leave(user: User): Group = copy(members = members - user)

  case class User(name: String)

object commands:
  enum Command:
    case Join(user: User)
    case Leave(user: User)

object events:
  enum Event:
    case Joined(user: User)
    case Left(user: User)

object eventsourcing:

  val groupsLocal =
    EventSourceLocal
      .of[Group, Command, Event](
        { case Command.Join(user) => Group(Set(user)) -> List(Event.Joined(user)) },
        (group, command) =>
          command match
            case Command.Join(user)  => ZIO.succeed(group.join(user) -> List(Event.Joined(user)))
            case Command.Leave(user) => ZIO.succeed(group.leave(user) -> List(Event.Left(user)))
        ,
        { case Event.Joined(user) => Group(Set(user)) },
        (group, event) =>
          event match
            case Event.Joined(user) => Right(group.join(user))
            case Event.Left(user)   => Right(group.leave(user)),
      )
      .makeLayer

  val inMemoryJournal = InMemoryJournal.makeLayer[Event]
```

You can find more examples under `examples` folder.

Contributing:
-------------

* To run tests: `sbt test`


[Link-CI]: https://github.com/palanga/parana/actions/workflows/ci.yml "CI"
[Link-SonatypeReleases]: https://s01.oss.sonatype.org/content/repositories/releases/io/github/palanga/parana-core-local_3/ "Sonatype Releases"
[Link-SonatypeSnapshots]: https://s01.oss.sonatype.org/content/repositories/snapshots/io/github/palanga/parana-core-local_3/ "Sonatype Snapshots"

[Badge-CI]: https://github.com/palanga/parana/actions/workflows/ci.yml/badge.svg "CI"
[Badge-SonatypeReleases]: https://img.shields.io/nexus/r/https/s01.oss.sonatype.org/io.github.palanga/parana-core-local_3.svg "Sonatype Releases"
[Badge-SonatypeSnapshots]: https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/io.github.palanga/parana-core-local_3.svg "Sonatype Snapshots"
