package palanga.zio

import java.util.UUID

package object eventsourcing {
  type AggregateId    = UUID
  type EventId        = UUID
  type Reducer[A, Ev] = (Option[A], Ev) => Either[Throwable, A]
}
