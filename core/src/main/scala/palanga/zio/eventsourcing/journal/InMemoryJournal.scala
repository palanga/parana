package palanga.zio.eventsourcing.journal

import palanga.zio.eventsourcing.AggregateId
import zio.stm.TMap
import zio.stream.ZStream
import zio.{ Chunk, Tag, ZIO, ZLayer }

import java.util.UUID

object InMemoryJournal {
  def layer[Ev](implicit etag: Tag[Ev]): ZLayer[Any, Nothing, Journal[Ev]] =
    TMap
      .empty[UUID, Chunk[Ev]]
      .commit
      .map(new InMemoryJournal[Ev](_))
      .toLayer
}

private[eventsourcing] class InMemoryJournal[Ev](private val eventsTMap: TMap[AggregateId, Chunk[Ev]])
    extends Journal.Service[Ev] {

  override def read(id: UUID): ZStream[Any, Nothing, Ev] =
    ZStream fromIterableM eventsTMap.getOrElse(id, Chunk.empty).commit

  override def write(event: (AggregateId, Ev)): ZIO[Any, Nothing, (AggregateId, Ev)] =
    eventsTMap.merge(event._1, Chunk(event._2))(_ ++ _).commit as event

  override def allIds: ZStream[Any, Nothing, AggregateId] =
    ZStream fromIterableM eventsTMap.keys.commit

}
