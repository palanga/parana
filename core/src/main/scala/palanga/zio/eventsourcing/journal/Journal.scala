package palanga.zio.eventsourcing.journal

import palanga.zio.eventsourcing.AggregateId
import zio._
import zio.stream.ZStream

import java.util.UUID

object Journal {

  trait Service[Ev] {
    def read(id: UUID): ZStream[Any, Throwable, Ev]
    def write(event: (AggregateId, Ev)): ZIO[Any, Throwable, (AggregateId, Ev)]
    def allIds: ZStream[Any, Throwable, UUID]
    def all: ZStream[Any, Throwable, (UUID, Chunk[Ev])] = allIds.mapM(id => read(id).runCollect.map(id -> _))
  }

}
