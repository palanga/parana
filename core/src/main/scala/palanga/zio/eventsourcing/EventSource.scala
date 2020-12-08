package palanga.zio.eventsourcing

import zio._
import zio.stream.{ Stream, ZStream }

import java.util.UUID

/**
 * Usage:
 * {{{
 * val painter = EventSource.read[Painter](painterId)
 *
 * val journalLayer = Journal.inMemory[Painter]
 * val eventSourceLayer = EventSource.live(applyEvent)
 * val appLayer = journalLayer >>> eventSourceLayer
 *
 * val painterWithProvidedDependencies = painter.provideLayer(appLayer)
 *
 * }}}
 */
object EventSource {

  type EventSource[A, Ev] = Has[Service[A, Ev]]

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
  )(implicit atag: Tag[A], etag: Tag[Ev]): ZLayer[Has[Journal[Ev]], Nothing, EventSource[A, Ev]] =
    ZLayer fromService (EventSource(_, f))

  def apply[A, Ev](db: Journal[Ev], f: ApplyEvent[A, Ev]): EventSource.Service[A, Ev] =
    new EventSourceLive(db, f)

  def write[A, Ev](
    eventResult: Either[Throwable, (AggregateId, Ev)]
  )(implicit atag: Tag[A], evtag: Tag[Ev]): ZIO[EventSource[A, Ev], Throwable, (AggregateId, Ev)] =
    ZIO.accessM(_.get.write(eventResult))

  def events[A, Ev](
    aggregateId: UUID
  )(implicit atag: Tag[A], evtag: Tag[Ev]): ZStream[EventSource[A, Ev], Throwable, Ev] =
    ZStream.accessStream(_.get.events(aggregateId))

  def read[A, Ev](
    aggregateId: UUID
  )(implicit atag: Tag[A], evtag: Tag[Ev]): ZIO[EventSource[A, Ev], Throwable, Option[A]] =
    ZIO.accessM(_.get.read(aggregateId))

  def readAndApplyCommand[A, Ev](
    id: UUID,
    command: A => Either[Throwable, Ev],
  )(implicit atag: Tag[A], evtag: Tag[Ev]): ZIO[EventSource[A, Ev], Throwable, Option[(AggregateId, Ev)]] =
    ZIO.accessM(_.get.readAndApplyCommand(id, command))

  trait Service[A, Ev] {
    def write(eventResult: Either[Throwable, (AggregateId, Ev)]): Task[(AggregateId, Ev)]
    def events(aggregateId: UUID): Stream[Throwable, Ev]
    def read(aggregateId: UUID): Task[Option[A]]
    def readAndApplyCommand(aggregateId: UUID, command: A => Either[Throwable, Ev]): Task[Option[(AggregateId, Ev)]]
  }

}

/**
 * The only difficult thing here is f: it takes a pair of optional aggregate and event. The None aggregate here
 * represents the initial state where the aggregate doesn't exist yet. Usually, in this case, the event
 * should be something like a creation event.
 * The result of this function must be either an error or the actual aggregate after applying the event successfully.
 *
 * @param db the event journal
 * @param f a function from an optional aggregate and an event to the result of applying that event to the aggregate
 * @tparam A the aggregate type
 * @tparam Ev the event type
 */
final class EventSourceLive[A, Ev] private[eventsourcing] (db: Journal[Ev], f: ApplyEvent[A, Ev])
    extends EventSource.Service[A, Ev] {

  override def write(commandResult: Either[Throwable, (AggregateId, Ev)]): Task[(AggregateId, Ev)] =
    ZIO
      .fromEither(commandResult)
      .flatMap(persist)

  override def events(aggregateId: UUID): Stream[Throwable, Ev] = db read aggregateId

  override def read(aggregateId: UUID): Task[Option[A]] =
    db.read(aggregateId)
      .fold(zero)(applyEvent(f))
      .flatMap(result => ZIO fromEither result)

  def readAndApplyCommand(aggregateId: UUID, command: A => Either[Throwable, Ev]): Task[Option[(AggregateId, Ev)]] =
    this
      .read(aggregateId)
      .some
      .map(command(_).left.map(Some(_)))
      .flatMap(ZIO fromEither _)
      .map(aggregateId -> _)
      .flatMap(persist(_).asSomeError)
      .optional

  private val zero: Either[Throwable, Option[A]] = Right(None)

  private def persist(event: (AggregateId, Ev)): ZIO[Any, Throwable, (AggregateId, Ev)] =
    db.write(event)
      .as(event)

  private def applyEvent(f: ApplyEvent[A, Ev])(prev: AggregateState, event: Ev): AggregateState =
    prev
      .flatMap(f(_, event))
      .map(Some(_))

  private type AggregateState = Either[Throwable, Option[A]]

}
