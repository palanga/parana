package palanga.parana.eventsource.local

import palanga.parana.eventsource.*
import palanga.parana.journal.*
import zio.*
import zio.stream.*

object EventSourceLocal:

  def makeLayer[A, Cmd, Ev](
    initCommand: PartialFunction[Cmd, (A, List[Ev])],
    applyCommand: (A, Cmd) => Task[(A, List[Ev])],
    initEvent: PartialFunction[Ev, A],
    applyEvent: (A, Ev) => Either[Throwable, A],
  )(using Tag[A], Tag[Cmd], Tag[Ev]): ZLayer[Journal[Ev], Nothing, EventSource[A, Cmd, Ev]] =
    ZLayer.scoped(ZIO.service[Journal[Ev]].map(EventSourceLocal(initCommand, applyCommand, initEvent, applyEvent, _)))

final class EventSourceLocal[A, Cmd, Ev](
  initCommand: PartialFunction[Cmd, (A, List[Ev])],
  applyCommand: (A, Cmd) => Task[(A, List[Ev])],
  initEvent: PartialFunction[Ev, A],
  applyEvent: (A, Ev) => Either[Throwable, A],
  journal: Journal[Ev],
)(using Tag[A], Tag[Cmd])
    extends EventSource[A, Cmd, Ev]:

  override def empty: EmptyEventSourcedEntity[A, Cmd, Ev] =
    EmptyEventSourcedEntityLocal(initCommand, journal)

  override def of(id: EntityId): EventSourcedEntity[A, Cmd, Ev] =
    EventSourcedEntityLocal(id, initEvent, applyEvent, applyCommand, journal)

final class EmptyEventSourcedEntityLocal[A, Cmd, Ev](
  init: PartialFunction[Cmd, (A, List[Ev])],
  journal: Journal[Ev],
)(using a: Tag[A], cmd: Tag[Cmd])
    extends EmptyEventSourcedEntity[A, Cmd, Ev]:

  override def ask(command: Cmd): Task[((EntityId, A), List[Ev])] =
    if !init.isDefinedAt(command) then ZIO.fail(Exception(s"Cannot init an ${a.tag.longName} from ${cmd.tag.longName}"))
    else
      val (newEntity, events) = init(command)
      for
        newEntityId <- Random.nextUUID
        _           <- journal.write(newEntityId, events.head) // TODO all events
      yield (newEntityId -> newEntity) -> events

final class EventSourcedEntityLocal[A, Cmd, Ev](
  id: EntityId,
  init: PartialFunction[Ev, A],
  applyEvent: (A, Ev) => Either[Throwable, A],
  applyCommand: (A, Cmd) => Task[(A, List[Ev])],
  journal: Journal[Ev],
) extends EventSourcedEntity[A, Cmd, Ev]:

  override def get: Task[A] =
    val events = journal.read(id)
    for
      head <- events.take(1).runHead.someOrFailException // take(1) first because runHead runs it until completion
      tail  = events.drop(1)
      a    <- tail.runFoldZIO(init(head))((a, ev) => ZIO.fromEither(applyEvent(a, ev))) // TODO optimize without ZIO
    yield a

  override def ask(command: Cmd): Task[(A, List[Ev])] =
    for
      a                  <- get
      (updatedA, events) <- applyCommand(a, command)
      _                  <- journal.write(id, events.head) // TODO all events
    yield updatedA -> events
