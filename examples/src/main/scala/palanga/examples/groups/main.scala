package palanga.examples.groups

import palanga.parana.eventsource.local.SOME_UUID // TODO
import zio.*

object app extends ZIOAppDefault:
  import model.*
  import commands.*
  import events.*
  import eventsourcing.*
  import model.*
  import palanga.parana.eventsource.*

  override def run = app.provide(groups)

  private val app =
    for
      groups <- ZIO.service[EventSource[Group, Command, Event]]
      _ <- groups.empty.ask(Command.Join(Person("Palan"))).debug
      _ <- groups.of(SOME_UUID).ask(Command.Join(Person("Nube"))).debug
      _ <- groups.of(SOME_UUID).ask(Command.Leave(Person("Palan"))).debug
      _ <- groups.of(SOME_UUID).ask(Command.Join(Person("Fruchi"))).debug
      _ <- groups.of(SOME_UUID).get.debug
    yield ()

object model:

  case class Group(members: Set[Person]):
    def join(person: Person): Group  = copy(members = members + person)
    def leave(person: Person): Group = copy(members = members - person)

  case class Person(name: String)

object commands:
  import model.*
  enum Command:
    case Join(person: Person)
    case Leave(person: Person)

object events:
  import model.*
  enum Event:
    case Joined(person: Person)
    case Left(person: Person)

object eventsourcing:
  import model.*
  import commands.*
  import events.*
  import palanga.parana.eventsource.local.*

  val groups: ZLayer[Any, Nothing, EventSourceLocal[Group, Command, Event]] =
    EventSourceLocal.makeLayer[Group, Command, Event](
      { case Command.Join(person) => Group(Set(person)) -> List(Event.Joined(person)) },
      (group, command) =>
        command match
          case Command.Join(person)  => ZIO.succeed(group.join(person) -> List(Event.Joined(person)))
          case Command.Leave(person) => ZIO.succeed(group.leave(person) -> List(Event.Left(person))),
    )
