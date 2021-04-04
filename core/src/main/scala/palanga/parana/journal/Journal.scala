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

case class JournalDecorator[Ev]() {

  def toLayer(implicit etag: Tag[Ev])   = raw.toLayer
  def toManaged(implicit etag: Tag[Ev]) = raw.toManaged_
  def raw(implicit etag: Tag[Ev])       = ZIO.environment[Journal[Ev]].map(_.get)

  /**
   * Return a new Journal that executes the given function every time an event is written (aka projection)
   */
  def tap[R](f: (AggregateId, Ev) => ZIO[R, Throwable, Any]) = JournalDecoratorR(f)

}

case class JournalDecoratorR[R, Ev](private val taps: (AggregateId, Ev) => ZIO[R, Throwable, Any]) {

  def toLayer(implicit etag: Tag[Ev])   = raw.toLayer
  def toManaged(implicit etag: Tag[Ev]) = raw.toManaged_
  def raw(implicit etag: Tag[Ev])       =
    for {
      journal <- ZIO.environment[Journal[Ev]]
      env     <- ZIO.environment[R]
    } yield journal.get.tap(taps(_, _).provide(env))

  /**
   * Return a new Journal that executes the given function every time an event is written (aka projection)
   */
  def tap[R1](f: (AggregateId, Ev) => ZIO[R1, Throwable, Any]) =
    copy(taps = (id: AggregateId, event: Ev) => this.taps(id, event) *> f(id, event))

  /**
   * Return a new Journal that executes the given function in parallel with the previously provided ones every time an
   * event is written (aka projection)
   */
  def tapPar[R1](f: (AggregateId, Ev) => ZIO[R1, Throwable, Any]) =
    copy(taps = (id: AggregateId, event: Ev) => this.taps(id, event) &> f(id, event))

}
