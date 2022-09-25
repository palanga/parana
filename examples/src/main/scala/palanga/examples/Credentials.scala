package palanga.examples

//import palanga.parana.types.AggregateId
import palanga.examples.v4.commands.Command.Close
import zio.*

import java.util.UUID

object Credentials:

  import palanga.parana.*

  case class Credential private (userId: UUID, login: Login):
    def changeLogin(newLogin: Login): Credential = this.copy(login = newLogin)
    def delete(): Credential                     = this

  object Credential:
    def create(login: Login): Credential = Credential(UUID.randomUUID(), login)

  sealed trait Login
  object Login:
    case class EmailLogin(address: String, password: String)     extends Login
    case class UsernameLogin(userName: String, password: String) extends Login

  sealed trait CredentialEvent
  object CredentialEvent:
    case class SignedUp(credential: Credential) extends CredentialEvent
    case class LoginUpdated(login: Login)       extends CredentialEvent
    case object Deleted                         extends CredentialEvent

  val reducer: Reducer[Credential, CredentialEvent] = (maybeCredential, event) =>
    maybeCredential match
      case Some(credential) =>
        event match
          case CredentialEvent.LoginUpdated(login) => Right(credential.changeLogin(login))
          case CredentialEvent.Deleted             => Right(credential)
          case _                                   => Left(Exception("Illegal state transition"))
      case None             =>
        event match
          case CredentialEvent.SignedUp(credential) => Right(credential)
          case _                                    => Left(Exception("Illegal state transition"))

  // http request with params and body -> (resource?, command) -> (resource, events)

  object CredentialsEventSource:

    val credentials = palanga.parana.EventSource.service[Credential, CredentialEvent]

    def signUp(login: Login): ZIO[EventSource[Credential, CredentialEvent], Throwable, (AggregateId, Credential)] =
      credentials.persistNewAggregateFromEvent(CredentialEvent.SignedUp(Credential.create(login)))

    def changeLogin(credentialId: UUID, newLogin: Login) = ???
    def delete(credentialId: UUID)                       = ???

object v2:

  import zio.*

  trait EventSourced[A, C, Ev]:

    def command(aggregateId: UUID, cmd: C): ZIO[Any, Throwable, (A, Seq[Ev])] =
      for
        agg              <- this.find(aggregateId)
        (newAgg, events) <- commander(agg, cmd)
        _                <- this.persist(events)
      yield newAgg -> events

    private def find(aggregateId: UUID): Task[A]     = ???
    private def persist(events: Seq[Ev]): Task[Unit] = ???

    protected def init(event: Ev): A
    protected def reducer(aggregate: A, event: Ev): A
    protected def commander[R, E](aggregate: A, cmd: C): ZIO[R, E, (A, Seq[Ev])]

  case class Credential private (userId: UUID, login: Login):
    def changeLogin(newLogin: Login): Credential = this.copy(login = newLogin)
    def delete(): Credential                     = this

  object Credential:
    def create(login: Login): Credential = Credential(UUID.randomUUID(), login)

  sealed trait Login
  object Login:
    case class EmailLogin(address: String, password: String)     extends Login
    case class UsernameLogin(userName: String, password: String) extends Login

  sealed trait CredentialEvent
  object CredentialEvent:
    case class SignedUp(credential: Credential) extends CredentialEvent
    case class LoginUpdated(login: Login)       extends CredentialEvent
    case object Deleted                         extends CredentialEvent

  sealed trait CredentialCommand
  object CredentialCommand:
    case class Create(login: Login)         extends CredentialCommand
    case class ChangeLogin(newLogin: Login) extends CredentialCommand
    case object Delete                      extends CredentialCommand

  class EventSourcedCredential extends EventSourced[Credential, CredentialCommand, CredentialEvent]:

    override def init(event: CredentialEvent): Credential = ???
//      event match
//        case CredentialEvent.SignedUp(credential) => credential

    override def reducer(aggregate: Credential, event: CredentialEvent): Credential =
      event match
        case CredentialEvent.LoginUpdated(login)  => aggregate.changeLogin(login)
        case CredentialEvent.Deleted              => aggregate.delete()
        case CredentialEvent.SignedUp(credential) => credential

    override def commander[R, E](
      aggregate: Credential,
      cmd: CredentialCommand,
    ): ZIO[R, E, (Credential, Seq[CredentialEvent])] =
      cmd match
        case CredentialCommand.Create(login)         =>
          val credential = Credential.create(login)
          ZIO.succeed(credential -> (CredentialEvent.SignedUp(credential) :: Nil))
        case CredentialCommand.ChangeLogin(newLogin) =>
          ZIO.succeed(aggregate.changeLogin(newLogin) -> (CredentialEvent.LoginUpdated(newLogin) :: Nil))
        case CredentialCommand.Delete                =>
          ZIO.succeed(aggregate.delete() -> (CredentialEvent.Deleted :: Nil))

  val credentials = EventSourcedCredential()

object v3 extends ZIOAppDefault:

  override def run = test()

  case class Credential private (userId: UUID, login: Login):
    def changeLogin(newLogin: Login): Credential = this.copy(login = newLogin)
    def delete(): Credential                     = this

  object Credential:
    def create(login: Login): Credential = Credential(UUID.randomUUID(), login)

  sealed trait Login
  object Login:
    case class EmailLogin(address: String, password: String)     extends Login
    case class UsernameLogin(userName: String, password: String) extends Login

  sealed trait CredentialEvent
  object CredentialEvent:
    case class SignedUp(credential: Credential) extends CredentialEvent
    case class LoginUpdated(login: Login)       extends CredentialEvent
    case object Deleted                         extends CredentialEvent

  sealed trait CredentialCommand
  object CredentialCommand:
    case class Create(login: Login)         extends CredentialCommand
    case class ChangeLogin(newLogin: Login) extends CredentialCommand
    case object Delete                      extends CredentialCommand

  class EventSourced[-R, +E >: Throwable, A, Command, CreateCommand, Event](
    creator: CreateCommand => (A, Event),
    init: Event => A,
    reduce: (A, Event) => A,
    command: (A, Command) => ZIO[R, E, (A, Seq[Event])],
  ):

    def of(aggregateId: UUID): EventSourcedEntity = EventSourcedEntity(aggregateId)

    def create(cmd: CreateCommand): Task[((UUID, A), (UUID, Event))] =
      val (aggregate, event) = creator(cmd)
      // create new aggregate uuid
      // create event time-uuid
      // persist aggregate uuid, event time-uuid and event
      // return aggregate uuid, event time-uuid and event
      ???

    class EventSourcedEntity(aggregateId: UUID):

      def ask(cmd: Command): ZIO[R, E, (A, Seq[Event])] =
        for
          aggregate              <- get
          (newAggregate, events) <- command(aggregate, cmd)
          _                      <- persist(events)
        yield newAggregate -> events

      def get: Task[A] = ???

      private def persist(events: Seq[Event]): Task[Unit] = ???

  val creator: CredentialCommand.Create => (Credential, CredentialEvent) =
    ???

  val init: CredentialEvent => Credential = event =>
    event match
      case CredentialEvent.SignedUp(credential) => credential
      case CredentialEvent.LoginUpdated(login)  => ???
      case CredentialEvent.Deleted              => ???

  val reduce: (Credential, CredentialEvent) => Credential = (credential, event) =>
    event match
      case CredentialEvent.SignedUp(cred)      => cred
      case CredentialEvent.LoginUpdated(login) => credential.changeLogin(login)
      case CredentialEvent.Deleted             => credential.delete()

  def command[R, E]: (Credential, CredentialCommand) => ZIO[R, E, (Credential, Seq[CredentialEvent])] =
    (credential, command) =>
      command match
        case CredentialCommand.Create(login)         =>
          val newCredential = Credential.create(login)
          ZIO.succeed(newCredential -> Seq(CredentialEvent.SignedUp(newCredential)))
        case CredentialCommand.ChangeLogin(newLogin) =>
          ZIO.succeed(credential.changeLogin(newLogin) -> Seq(CredentialEvent.LoginUpdated(newLogin)))
        case CredentialCommand.Delete                =>
          ZIO.succeed(credential.delete() -> Seq(CredentialEvent.Deleted))

  val credentials = EventSourced(creator, init, reduce, command)

  def test() =
    import zio.test.*
    for
      ((credId, cred), _) <- credentials.create(CredentialCommand.Create(Login.EmailLogin("address@lala.com", "pase")))
      credAgain           <- credentials.of(credId).get
      (updatedCred, _)    <- credentials
                               .of(credId)
                               .ask(CredentialCommand.ChangeLogin(Login.UsernameLogin("palanga", "nubebebe")))
      updatedCredAgain    <- credentials.of(credId).get
    yield assertTrue {
      cred == credAgain
      && updatedCred == updatedCredAgain
    }

object v4:

  object model:

    case class Order(items: List[(Item, Amount)], status: Status):
      def addItems(items: List[Item]): Either[Error, Order]   = Right(this.copy(List(items.head -> items.size))) // TODO
      def removeItems(item: List[Item]): Either[Error, Order] = ???
      def total: Price                                        = items.map((item, amount) => item.price * amount).sum

      def pay(amount: Price): Either[Error, Order] =
        val newStatus = status match
          case Status.Paid | Status.Done => Left(Error.IllegalStatusTransition)
          case Status.Opened             => if amount == total then Right(Status.Paid) else Left(Error.InsufficientFunds)
          case Status.Delivered          => if amount == total then Right(Status.Done) else Left(Error.InsufficientFunds)

        newStatus.map(newStatus => this.copy(status = newStatus))

      def deliver(to: String): Either[Error, Order] = ???

      def close: Either[Error, Order] = ???

    case class Item(name: String, price: Price)

    enum Status:
      case Opened, Paid, Delivered, Done

    enum Error extends Throwable:
      case IllegalState, InsufficientFunds, IllegalStatusTransition

    type Price  = BigDecimal
    type Amount = Int

  object events:
    import model.*
    enum Event:
      case ItemsAdded(items: List[Item])
      case ItemsRemoved(items: List[Item])
      case Paid(amount: Price)
      case Delivered(to: String)
      case Closed

  object commands:
    import model.*
    enum Command:
      case AddItems(items: List[Item])
      case RemoveItems(items: List[Item])
      case Pay(amount: Price)
      case Deliver(to: String)
      case Close

  object eventsourcing:
    import model.*
    import commands.*
    import events.*

    val initCommand: PartialFunction[Command, (Order, List[Event])] =
      case Command.AddItems(items) =>
        Order(Nil, Status.Opened).addItems(items).getOrElse(???) -> List(Event.ItemsAdded(items))

    def applyCommand(order: Order, cmd: Command): Task[(Order, List[Event])] =
      ZIO
        .fromEither(commandToMethod(cmd)(order))
        .map(_ -> (commandToEvent(cmd) :: Nil))
        .mapError(e => new Exception(e.toString, e))

    private def commandToEvent(cmd: Command): Event = cmd match
      case Command.AddItems(items)    => Event.ItemsAdded(items)
      case Command.RemoveItems(items) => Event.ItemsRemoved(items)
      case Command.Pay(amount)        => Event.Paid(amount)
      case Command.Deliver(to)        => Event.Delivered(to)
      case Command.Close              => Event.Closed

    private def commandToMethod(cmd: Command): Order => Either[Error, Order] = order =>
      cmd match
        case Command.AddItems(items)    => order.addItems(items)
        case Command.RemoveItems(items) => order.removeItems(items)
        case Command.Pay(amount)        => order.pay(amount)
        case Command.Deliver(to)        => order.deliver(to)
        case Command.Close              => order.close

  object eventsource:

    trait EventSource[A, Cmd, Ev]:
      def empty: EmptyEventSourcedEntity[A, Cmd, Ev]

      def of(id: String): EventSourcedEntity[A, Cmd, Ev]

    trait EventSourcedEntity[A, Cmd, Ev]:
      def get: Task[A]

      def ask(cmd: Cmd): Task[(A, List[Ev])]

    trait EmptyEventSourcedEntity[A, Cmd, Ev]:
      def ask(cmd: Cmd): Task[(A, List[Ev])] // TODO return new uuid

  object example extends ZIOAppDefault:

    import model.*
    import commands.*
    import events.*
    import eventsource.*
    import eventsourcing.*
    import eventsource_local.*



    override def run: ZIO[Any, Any, Any] =
      app.provide(EventSourceLocal.makeLayer(initCommand, applyCommand))

    val app: ZIO[EventSource[Order, Command, Event], Throwable, Order] =
      for
        orders <- ZIO.service[EventSource[Order, Command, Event]]
        _      <- orders.empty.ask(Command.AddItems(Item("hueso", 10) :: Nil)).debug
        _      <- orders.of("entity uuid").get.map(_.total).debug
        _      <- orders.of("entity uuid").ask(Command.Pay(10)).debug
        order  <- orders.of("entity uuid").get.debug
      yield order

  object eventsource_local:

    import eventsource.*

    object EventSourceLocal:

      def makeLayer[A, Cmd, Ev](
        initCommand: PartialFunction[Cmd, (A, List[Ev])],
        applyCommand: (A, Cmd) => Task[(A, List[Ev])],
      )(using Tag[A], Tag[Cmd], Tag[Ev]): ZLayer[Any, Nothing, EventSourceLocal[A, Cmd, Ev]] =
        ZLayer.fromZIO(make(initCommand, applyCommand))

      def make[A, Cmd, Ev](
        initCommand: PartialFunction[Cmd, (A, List[Ev])],
        applyCommand: (A, Cmd) => Task[(A, List[Ev])],
      ): ZIO[Any, Nothing, EventSourceLocal[A, Cmd, Ev]] =
        Ref.make(Map.empty[EntityId, A]).map(state => EventSourceLocal(initCommand, state, applyCommand))

    final class EventSourceLocal[A, Cmd, Ev]( //
      initCommand: PartialFunction[Cmd, (A, List[Ev])],
      state: Ref[Map[EntityId, A]],
      applyCommand: (A, Cmd) => Task[(A, List[Ev])],
    ) extends EventSource[A, Cmd, Ev]:

      override def empty: EmptyEventSourcedEntity[A, Cmd, Ev] = EmptyEventSourcedEntityLocal(initCommand, state)

      override def of(id: String): EventSourcedEntity[A, Cmd, Ev] = EventSourcedEntityLocal(id, state, applyCommand)

    final class EmptyEventSourcedEntityLocal[A, Cmd, Ev]( //
      init: PartialFunction[Cmd, (A, List[Ev])],
      state: Ref[Map[EntityId, A]],
    ) extends EmptyEventSourcedEntity[A, Cmd, Ev]:

      override def ask(cmd: Cmd): Task[(A, List[Ev])] =
        if init.isDefinedAt(cmd)
        then
          val (newEntity, events) = init(cmd)
          val uuid                = "entity uuid"
          state.update(_ + (uuid -> newEntity)).as(newEntity, events)
        else ZIO.fail(new Exception("invalid command"))

    final class EventSourcedEntityLocal[A, Cmd, Ev](
      id: EntityId,
      state: Ref[Map[EntityId, A]],
      applyCommand: (A, Cmd) => Task[(A, List[Ev])],
    ) extends EventSourcedEntity[A, Cmd, Ev]:

      override def get: Task[A] = state.get.map(_.get(id)).someOrFailException

      override def ask(cmd: Cmd): Task[(A, List[Ev])] = get.flatMap(applyCommand(_, cmd))

    type EntityId = String
