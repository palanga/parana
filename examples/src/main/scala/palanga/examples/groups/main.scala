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

  override def run = app.provide(groupsLocal, inMemoryJournal).debug

  private val app =
    for
      groups       <- ZIO.service[EventSource[Group, Command, Event]]
      ((id, _), _) <- groups.empty.ask(Command.Join(User("Palan")))
      _            <- groups.of(id).ask(Command.Join(User("Nube")))
      _            <- groups.of(id).ask(Command.Leave(User("Palan")))
      _            <- groups.of(id).ask(Command.Join(User("Fruchi")))
      group        <- groups.of(id).get
    yield group.members == Set(User("Nube"), User("Fruchi"))

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
