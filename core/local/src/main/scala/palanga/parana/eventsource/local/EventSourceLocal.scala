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
  )(using Tag[A], Tag[Cmd]): ZIO[Any, Nothing, EventSourceLocal[A, Cmd, Ev]] =
    Ref.make(Map.empty[EntityId, A]).map(state => EventSourceLocal(initCommand, state, applyCommand))

final class EventSourceLocal[A, Cmd, Ev](
  initCommand: PartialFunction[Cmd, (A, List[Ev])],
  state: Ref[Map[EntityId, A]],
  applyCommand: (A, Cmd) => Task[(A, List[Ev])],
)(using Tag[A], Tag[Cmd])
    extends EventSource[A, Cmd, Ev]:

  override def empty: EmptyEventSourcedEntity[A, Cmd, Ev] = EmptyEventSourcedEntityLocal(initCommand, state)

  override def of(id: EntityId): EventSourcedEntity[A, Cmd, Ev] = EventSourcedEntityLocal(id, state, applyCommand)

final class EmptyEventSourcedEntityLocal[A, Cmd, Ev](
  init: PartialFunction[Cmd, (A, List[Ev])],
  state: Ref[Map[EntityId, A]],
)(using a: Tag[A], cmd: Tag[Cmd])
    extends EmptyEventSourcedEntity[A, Cmd, Ev]:

  override def ask(command: Cmd): Task[((EntityId, A), List[Ev])] =
    if !init.isDefinedAt(command) then ZIO.fail(Exception(s"Cannot init an ${a.tag.longName} from ${cmd.tag.longName}"))
    else
      val (newEntity, events) = init(command)
      for
        newEntityId <- Random.nextUUID
        _           <- state.update(_ + (newEntityId -> newEntity))
      yield (newEntityId -> newEntity) -> events

final class EventSourcedEntityLocal[A, Cmd, Ev](
  id: EntityId,
  state: Ref[Map[EntityId, A]],
  applyCommand: (A, Cmd) => Task[(A, List[Ev])],
) extends EventSourcedEntity[A, Cmd, Ev]:

  override def get: Task[A] = state.get.map(_.get(id)).someOrFailException

  override def ask(command: Cmd): Task[(A, List[Ev])] =
    for
      a                  <- get
      (updatedA, events) <- applyCommand(a, command)
      _                  <- state.update(_ + (id -> updatedA))
    yield updatedA -> events

val SOME_UUID = java.util.UUID.fromString("e6503810-3dcc-11ed-b878-0242ac120002")
