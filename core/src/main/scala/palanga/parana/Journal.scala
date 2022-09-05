package palanga.parana

import palanga.parana.types.*
import zio.*
import zio.stream.ZStream

trait Journal[Ev] { self =>

  def read(id: AggregateId): ZStream[Any, Throwable, Ev]
  def write(id: AggregateId, event: Ev): ZIO[Any, Throwable, (AggregateId, Ev)]
  def allIds: ZStream[Any, Throwable, AggregateId]

  def all: ZStream[Any, Throwable, (AggregateId, Chunk[Ev])] = allIds.mapZIO(id => read(id).runCollect.map(id -> _))

  /**
   * Return a new Journal that executes the given function every time an event is written (aka projection)
   */
  def tap(f: (AggregateId, Ev) => IO[Throwable, Any]) = new Journal[Ev] {
    override def read(id: AggregateId)             = self.read(id)
    override def write(id: AggregateId, event: Ev) = self.write(id, event).tap(r => f(r._1, r._2))
    override def allIds                            = self.allIds
  }

}

object Journal:

  def inMemory[Ev](implicit etag: Tag[Ev]): ZIO[Any, Nothing, Journal[Ev]] = InMemoryJournal.make

  def decorator[Ev](implicit etag: Tag[Ev]): JournalDecorator[Ev] = JournalDecorator()

case class JournalDecorator[Ev]() {

  /**
   * Return a new Journal that executes the given function every time an event is written (aka projection)
   */
  def tap[R](f: (AggregateId, Ev) => ZIO[R, Throwable, Any]) = JournalDecoratorR(f)

}

case class JournalDecoratorR[R, Ev](private val taps: (AggregateId, Ev) => ZIO[R, Throwable, Any]) {

  def decoratePure(journal: Journal[Ev]) =
    ZIO.environment[R].map(env => journal.tap(taps(_, _).provideEnvironment(env)))

  def decorate[R1](journal: ZIO[R1, Throwable, Journal[Ev]]) = journal.flatMap(decoratePure)

  def toLayer(implicit etag: Tag[Ev]) = ZLayer.apply(raw)
  def raw(implicit etag: Tag[Ev])     = ZIO.service[Journal[Ev]].flatMap(decoratePure)

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
