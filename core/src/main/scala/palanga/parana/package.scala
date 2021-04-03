package palanga

import java.util.UUID

package object parana {
  type AggregateId    = UUID
  type EventId        = UUID
  type Reducer[A, Ev] = (Option[A], Ev) => Either[Throwable, A]
}
