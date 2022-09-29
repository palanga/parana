package palanga.parana.eventsource

import zio.*

import java.util.UUID

// TODO use version 1, time-based (no MAC address) UUID for event ids
trait EventSource[A, Cmd, Ev]:
  def empty: EmptyEventSourcedEntity[A, Cmd, Ev]
  def of(id: EntityId): EventSourcedEntity[A, Cmd, Ev]

trait EventSourcedEntity[A, Cmd, Ev]:
  def get: Task[A]
  def ask(command: Cmd): Task[(A, List[Ev])]

trait EmptyEventSourcedEntity[A, Cmd, Ev]:
  def ask(command: Cmd): Task[((EntityId, A), List[Ev])]

type EntityId = UUID // TODO use version 4, random UUID opaque type
