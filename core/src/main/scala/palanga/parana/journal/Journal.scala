package palanga.parana.journal

import palanga.parana.AggregateId
import zio._
import zio.stream.ZStream

object Journal {

  trait Service[Ev] { self =>

    def read(id: AggregateId): ZStream[Any, Throwable, Ev]
    def write(id: AggregateId, event: Ev): ZIO[Any, Throwable, (AggregateId, Ev)]
    def allIds: ZStream[Any, Throwable, AggregateId]

    def all: ZStream[Any, Throwable, (AggregateId, Chunk[Ev])] = allIds.mapM(id => read(id).runCollect.map(id -> _))

    /**
     * Return a new Journal that executes the given function every time an event is written (aka projection)
     */
    def tap(f: (AggregateId, Ev) => IO[Throwable, Any]) = new Service[Ev] {
      override def read(id: AggregateId)             = self.read(id)
      override def write(id: AggregateId, event: Ev) = self.write(id, event).tap(r => f(r._1, r._2))
      override def allIds                            = self.allIds
    }

  }

}

case class JournalBuilder[Ev](private val journal: IO[Throwable, Journal.Service[Ev]]) extends AnyVal {

  def layer(implicit etag: Tag[Ev]): ZLayer[Any, Throwable, Journal[Ev]] = journal.toLayer
  def managed: ZManaged[Any, Throwable, Journal.Service[Ev]]             = journal.toManaged_
  def raw: IO[Throwable, Journal.Service[Ev]]                            = journal

  /**
   * Return a new Journal that executes the given function every time an event is written (aka projection)
   */
  def tap(f: (AggregateId, Ev) => IO[Throwable, Any]) = copy(journal.map(_.tap(f)))

}
