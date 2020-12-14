package palanga.zio.eventsourcing

import zio._
import zio.stm.TMap
import zio.stream.ZStream

import java.util.UUID

object Journal {

  type Journal[Ev] = Has[Service[Ev]]

  def inMemory[Ev](implicit etag: Tag[Ev]): ZLayer[Any, Nothing, Journal[Ev]] =
    TMap
      .empty[UUID, Chunk[Ev]]
      .commit
      .map { eventsTMap =>
        new Service[Ev] {

          override def read(id: UUID): ZStream[Any, Throwable, Ev] =
            ZStream fromIterableM eventsTMap.getOrElse(id, Chunk.empty).commit

          override def write(event: (AggregateId, Ev)): ZIO[Any, Throwable, (AggregateId, Ev)] =
            eventsTMap.merge(event._1, Chunk(event._2))(_ ++ _).commit as event

          override def all: ZStream[Any, Throwable, (UUID, Chunk[Ev])] =
            ZStream fromIterableM eventsTMap.toChunk.commit

        }
      }
      .toLayer

  trait Service[Ev] {
    def read(id: UUID): ZStream[Any, Throwable, Ev]
    def write(event: (AggregateId, Ev)): ZIO[Any, Throwable, (AggregateId, Ev)]
    def all: ZStream[Any, Throwable, (UUID, Chunk[Ev])]
  }

}
