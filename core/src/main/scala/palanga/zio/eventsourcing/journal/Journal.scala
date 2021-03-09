package palanga.zio.eventsourcing.journal

import palanga.zio.eventsourcing.AggregateId
import zio._
import zio.stream.ZStream

object Journal {

  trait Service[Ev] {
    def read(id: AggregateId): ZStream[Any, Throwable, Ev]
    def write(id: AggregateId, event: Ev): ZIO[Any, Throwable, (AggregateId, Ev)]
    def allIds: ZStream[Any, Throwable, AggregateId]
    def all: ZStream[Any, Throwable, (AggregateId, Chunk[Ev])] = allIds.mapM(id => read(id).runCollect.map(id -> _))
  }

}
