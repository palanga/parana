package palanga.parana.journal

import palanga.parana.eventsource.*
import zio.*
import zio.stream.*

// TODO rethink
trait Journal[Ev] { self =>

  def read(id: EntityId): ZStream[Any, Throwable, Ev]
  def write(id: EntityId, event: Ev): ZIO[Any, Throwable, (EntityId, Ev)]
  def allIds: ZStream[Any, Throwable, EntityId]

  def all: ZStream[Any, Throwable, (EntityId, Chunk[Ev])] = allIds.mapZIO(id => read(id).runCollect.map(id -> _))

  /**
   * Return a new Journal that executes the given function every time an event is written
   */
  private[parana] def withProjection(f: (EntityId, Ev) => Task[Unit]) = new Journal[Ev] {
    override def read(id: EntityId)             = self.read(id)
    override def write(id: EntityId, event: Ev) = self.write(id, event).tap(f(_, _))
    override def allIds                         = self.allIds
  }

}
