package palanga.zio

import java.util.UUID

package object eventsourcing {
  type AggregateId       = UUID
  type ApplyEvent[A, Ev] = (Option[A], Ev) => Either[Throwable, A]
}
