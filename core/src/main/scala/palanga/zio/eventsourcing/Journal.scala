package palanga.zio.eventsourcing

import zio._
import zio.stm.TMap
import zio.stream.ZStream

import java.util.UUID

trait Journal[Ev] {
  def read(id: UUID): ZStream[Any, Throwable, Ev]
  def write(event: (AggregateId, Ev)): ZIO[Any, Throwable, Ev]
}

object Journal {

  def inMemory[Ev](implicit etag: Tag[Ev]): ZLayer[Any, Nothing, Has[Journal[Ev]]] =
    TMap
      .empty[UUID, Chunk[Ev]]
      .commit
      .map { eventsTMap =>
        new Journal[Ev] {
          override def read(id: UUID): ZStream[Any, Throwable, Ev]              =
            ZStream fromIterableM eventsTMap.getOrElse(id, Chunk.empty).commit
          override def write(event: (AggregateId, Ev)): ZIO[Any, Throwable, Ev] =
            eventsTMap.merge(event._1, Chunk(event._2))(_ ++ _).commit as event._2
        }
      }
      .toLayer

}
