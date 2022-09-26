package palanga.examples

import zio.*

// TODO return event id
object eventsource:

  trait EventSource[A, Cmd, Ev]:
    def empty: EmptyEventSourcedEntity[A, Cmd, Ev]
    def of(id: EntityId): EventSourcedEntity[A, Cmd, Ev]

  trait EventSourcedEntity[A, Cmd, Ev]:
    def get: Task[A]
    def ask(cmd: Cmd): Task[(A, List[Ev])]

  trait EmptyEventSourcedEntity[A, Cmd, Ev]:
    def ask(cmd: Cmd): Task[(A, List[Ev])] // TODO: return new entity id

  type EntityId = String
