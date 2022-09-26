package palanga.parana.eventsource.local

import palanga.parana.eventsource.*
import zio.*

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

  override def of(id: EntityId): EventSourcedEntity[A, Cmd, Ev] = EventSourcedEntityLocal(id, state, applyCommand)

final class EmptyEventSourcedEntityLocal[A, Cmd, Ev]( //
  init: PartialFunction[Cmd, (A, List[Ev])],
  state: Ref[Map[EntityId, A]],
) extends EmptyEventSourcedEntity[A, Cmd, Ev]:

  override def ask(cmd: Cmd): Task[(A, List[Ev])] =
    if init.isDefinedAt(cmd)
    then
      val (newEntity, events) = init(cmd)
      val uuid                = SOME_UUID // TODO
      state.update(_ + (uuid -> newEntity)).as(newEntity, events)
    else ZIO.fail(new Exception("invalid command"))

final class EventSourcedEntityLocal[A, Cmd, Ev](
  id: EntityId,
  state: Ref[Map[EntityId, A]],
  applyCommand: (A, Cmd) => Task[(A, List[Ev])],
) extends EventSourcedEntity[A, Cmd, Ev]:

  override def get: Task[A] = state.get.map(_.get(id)).someOrFailException

  override def ask(cmd: Cmd): Task[(A, List[Ev])] =
    for
      a                  <- get
      (updatedA, events) <- applyCommand(a, cmd)
      _                  <- state.update(_ + (id -> updatedA))
    yield updatedA -> events

val SOME_UUID = java.util.UUID.fromString("e6503810-3dcc-11ed-b878-0242ac120002")
