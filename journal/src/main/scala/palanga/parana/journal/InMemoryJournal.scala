package palanga.parana.journal

import palanga.parana.eventsource.*
import zio.stm.*
import zio.stream.*
import zio.*

object InMemoryJournal {
  def make[Ev]: ZIO[Any, Nothing, Journal[Ev]] =
    TMap
      .empty[EntityId, Chunk[Ev]]
      .commit
      .map(new InMemoryJournal[Ev](_))
}

private[parana] class InMemoryJournal[Ev](private val eventsTMap: TMap[EntityId, Chunk[Ev]]) extends Journal[Ev] {

  override def read(id: EntityId): ZStream[Any, Nothing, Ev] =
    ZStream.fromIterableZIO(eventsTMap.getOrElse(id, Chunk.empty).commit)

  override def write(id: EntityId, event: Ev): ZIO[Any, Nothing, (EntityId, Ev)] =
    eventsTMap.merge(id, Chunk(event))(_ ++ _).commit.as(id -> event)

  override def allIds: ZStream[Any, Nothing, EntityId] =
    ZStream.fromIterableZIO(eventsTMap.keys.commit)

}
