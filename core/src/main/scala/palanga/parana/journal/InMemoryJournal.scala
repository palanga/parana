package palanga.parana.journal

import palanga.parana.AggregateId
import zio.stm.TMap
import zio.stream.ZStream
import zio.{ Chunk, ZIO }

import java.util.UUID

object InMemoryJournal {
  def make[Ev]: ZIO[Any, Nothing, Journal.Service[Ev]] =
    TMap
      .empty[UUID, Chunk[Ev]]
      .commit
      .map(new InMemoryJournal[Ev](_))
}

private[parana] class InMemoryJournal[Ev](private val eventsTMap: TMap[AggregateId, Chunk[Ev]])
    extends Journal.Service[Ev] {

  override def read(id: UUID): ZStream[Any, Nothing, Ev] =
    ZStream fromIterableM eventsTMap.getOrElse(id, Chunk.empty).commit

  override def write(id: AggregateId, event: Ev): ZIO[Any, Nothing, (AggregateId, Ev)] =
    eventsTMap.merge(id, Chunk(event))(_ ++ _).commit as (id -> event)

  override def allIds: ZStream[Any, Nothing, AggregateId] =
    ZStream fromIterableM eventsTMap.keys.commit

}
