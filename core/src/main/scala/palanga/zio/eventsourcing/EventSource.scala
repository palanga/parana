package palanga.zio.eventsourcing

import palanga.zio.eventsourcing.Journal.Journal
import zio._
import zio.stream.{ Stream, ZStream }

import java.util.UUID

/**
 * Example usage:
 * {{{
 *
 * val painters = EventSource.of[Painter]
 *
 * val painter = painters.read(painterId)
 *
 * val journalLayer = Journal.inMemory[Painter]
 * val eventSourceLayer = EventSource.live(applyEvent)
 * val appLayer = journalLayer >>> eventSourceLayer
 *
 * val painterWithProvidedDependencies = painter.provideLayer(appLayer)
 * }}}
 */
object EventSource {

  type EventSource[A, Ev] = Has[Service[A, Ev]]

  def of[A, Ev](implicit aTag: Tag[A], eTag: Tag[Ev]): EventSourceOf[A, Ev] = new EventSourceOf[A, Ev]()

  /**
   * Create a [[ZLayer]] from [[Journal]] to [[EventSource]]
   *
   * Example:
   * {{{
   * def applyEvent: ApplyEvent[Painter, Event] = {
   *   case (None, Event.Created(painter))               =>
   *     Right(painter)
   *   case (Some(painter), added: Event.PaintingsAdded) =>
   *     Right(painter.copy(painter.paintings ++ added.paintings))
   *   case (maybePainter, event)                        =>
   *     Left(new Exception(s"$maybePainter $event"))
   * }
   * }}}
   *
   * @param f The function used to recover an aggregate from events
   * @tparam A The type of the aggregate
   * @tparam Ev The type of the event
   */
  def live[A, Ev](
    f: ApplyEvent[A, Ev]
  )(implicit atag: Tag[A], etag: Tag[Ev]): ZLayer[Journal[Ev], Nothing, EventSource[A, Ev]] =
    ZLayer fromService (EventSource(_, f))

  /**
   * Consider using [[live]] instead to construct a ZLayer.
   */
  def apply[A, Ev](db: Journal.Service[Ev], f: ApplyEvent[A, Ev]): EventSource.Service[A, Ev] =
    new EventSourceLive(db, f)

  final class EventSourceOf[A, Ev](implicit val aTag: Tag[A], val eTag: Tag[Ev]) {

    def write(
      eventResult: Either[Throwable, (AggregateId, Ev)]
    ): ZIO[EventSource[A, Ev], Throwable, (AggregateId, Ev)] =
      ZIO.accessM(_.get.write(eventResult))

    def read(aggregateId: UUID): ZIO[EventSource[A, Ev], Throwable, Option[A]] =
      ZIO.accessM(_.get.read(aggregateId))

    def readAndApplyCommand(
      id: UUID,
      command: A => Either[Throwable, Ev],
    ): ZIO[EventSource[A, Ev], Throwable, Option[(AggregateId, Ev)]] =
      ZIO.accessM(_.get.readAndApplyCommand(id, command))

    def readAll: ZStream[EventSource[A, Ev], Throwable, A] =
      ZStream.accessStream(_.get.readAll)

    def events(aggregateId: UUID): ZStream[EventSource[A, Ev], Throwable, Ev] =
      ZStream.accessStream(_.get.events(aggregateId))

  }

  trait Service[A, Ev] {
    def write(eventResult: Either[Throwable, (AggregateId, Ev)]): Task[(AggregateId, Ev)]
    def read(aggregateId: UUID): Task[Option[A]]
    def readAndApplyCommand(aggregateId: UUID, command: A => Either[Throwable, Ev]): Task[Option[(AggregateId, Ev)]]
    def readAll: Stream[Throwable, A]
    def events(aggregateId: UUID): Stream[Throwable, Ev]
  }

}

final class EventSourceLive[A, Ev] private[eventsourcing] (db: Journal.Service[Ev], f: ApplyEvent[A, Ev])
    extends EventSource.Service[A, Ev] {

  override def write(commandResult: Either[Throwable, (AggregateId, Ev)]): Task[(AggregateId, Ev)] =
    ZIO
      .fromEither(commandResult)
      .flatMap(db.write)

  override def read(aggregateId: UUID): Task[Option[A]] =
    db.read(aggregateId)
      .fold(zero)(applyEvent(f))
      .flatMap(ZIO fromEither _)

  override def readAndApplyCommand(
    aggregateId: UUID,
    command: A => Either[Throwable, Ev],
  ): Task[Option[(AggregateId, Ev)]] =
    this
      .read(aggregateId)
      .some
      .map(command(_).left.map(Some(_)))
      .flatMap(ZIO fromEither _)
      .map(aggregateId -> _)
      .flatMap(db.write(_).asSomeError)
      .optional

  override def readAll: Stream[Throwable, A]                    =
    db.all.map { case (_, events) => events.foldLeft(zero)(applyEvent(f)) }
      .mapM(ZIO fromEither _)
      .collect { case Some(a) => a }

  override def events(aggregateId: UUID): Stream[Throwable, Ev] = db read aggregateId

  private val zero: Either[Throwable, Option[A]] = Right(None)

  private def applyEvent(f: ApplyEvent[A, Ev])(prev: AggregateState, event: Ev): AggregateState =
    prev
      .flatMap(f(_, event))
      .map(Some(_))

  private type AggregateState = Either[Throwable, Option[A]]

}
