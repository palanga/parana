package palanga.parana.eventsource

import CoreSpec.model.*
import CoreSpec.commands.*
import CoreSpec.events.*
import CoreSpec.eventsourcing.*
import palanga.parana.eventsource.*
import zio.ZIO
import zio.test.*

// TODO projection tests
object CoreSpec:

  private val groupsService = ZIO.service[EventSource[Group, Command, Event]]

  val testSuite: Spec[EventSource[Group, Command, Event], Throwable] =
    suite("an event source")(
      test("can create a new entity") {
        for
          groups               <- groupsService
          ((_, group), events) <- groups.empty.ask(Command.Join(User("Nube")))
        yield assertTrue {
          group == Group(Set(User("Nube")))
          && events == List(Event.Joined(User("Nube")))
        }
      },
      test("can get a created entity") {
        for
          groups       <- groupsService
          ((id, _), _) <- groups.empty.ask(Command.Join(User("Nube")))
          group        <- groups.of(id).get
        yield assertTrue {
          group == Group(Set(User("Nube")))
        }
      },
      test("can ask to a created entity") {
        for
          groups          <- groupsService
          ((id, _), _)    <- groups.empty.ask(Command.Join(User("Nube")))
          (group, events) <- groups.of(id).ask(Command.Leave(User("Nube")))
        yield assertTrue {
          group == Group(Set())
          && events == List(Event.Left(User("Nube")))
        }
      },
      test("can create to different new entities") {
        for
          groups        <- groupsService
          ((id1, _), _) <- groups.empty.ask(Command.Join(User("Nube")))
          ((id2, _), _) <- groups.empty.ask(Command.Join(User("Nube")))
        yield assertTrue {
          id1 != id2
        }
      },
      test("can ask to two different entities") {
        for
          groups            <- groupsService
          ((id1, _), _)     <- groups.empty.ask(Command.Join(User("Nube")))
          ((id2, _), _)     <- groups.empty.ask(Command.Join(User("Nube")))
          (group1, events1) <- groups.of(id1).ask(Command.Join(User("Fruchi")))
          (group2, events2) <- groups.of(id2).ask(Command.Join(User("Palan")))
        yield assertTrue {
          id1 != id2
          && group1 == Group(Set(User("Nube"), User("Fruchi")))
          && group2 == Group(Set(User("Nube"), User("Palan")))
          && events1 == List(Event.Joined(User("Fruchi")))
          && events2 == List(Event.Joined(User("Palan")))
        }
      },
    )

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

    val initCommand: PartialFunction[Command, (Group, List[Event])] = { case Command.Join(user) =>
      Group(Set(user)) -> List(Event.Joined(user))
    }

    def applyCommand(group: Group, command: Command): ZIO[Any, Nothing, (Group, List[Event])] =
      command match
        case Command.Join(user)  => ZIO.succeed(group.join(user) -> List(Event.Joined(user)))
        case Command.Leave(user) => ZIO.succeed(group.leave(user) -> List(Event.Left(user)))

    val initEvent: PartialFunction[Event, Group] = { case Event.Joined(user) =>
      Group(Set(user))
    }

    def applyEvent(group: Group, event: Event): Right[Nothing, Group] =
      event match
        case Event.Joined(user) => Right(group.join(user))
        case Event.Left(user)   => Right(group.leave(user))
