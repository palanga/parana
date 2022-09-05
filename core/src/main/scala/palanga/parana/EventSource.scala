package palanga.parana

import palanga.parana.types.*
import zio.*
import zio.stream.*

import java.util.{ NoSuchElementException, UUID }

type Reducer[A, Ev] = (Option[A], Ev) => Either[Throwable, A]

trait EventSource[A, Ev] {
  def persistNewAggregateFromEvent(event: Ev): Task[(AggregateId, A)]
  def persist(uuid: AggregateId)(event: Ev): Task[A]
  def persistEither(uuid: AggregateId)(command: A => Either[Throwable, Ev]): Task[A]
  def read(uuid: AggregateId): Task[A]
  def readOption(uuid: AggregateId): Task[Option[A]]
  def readAll: Stream[Throwable, (AggregateId, A)]
  def events(uuid: AggregateId): Stream[Throwable, Ev]
}

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
   * @param reduce
   *   The function used to recover an aggregate from events
   * @tparam A
   *   The type of the aggregate
   * @tparam Ev
   *   The type of the event
   */
  def live[A, Ev](
    reduce: Reducer[A, Ev]
  )(implicit atag: Tag[A], etag: Tag[Ev]): ZLayer[Journal[Ev], Nothing, EventSource[A, Ev]] =
    ZLayer.apply(ZIO.environment[Journal[Ev]].map(j => EventSource(j.get, reduce)))

  /**
   * Consider using [[live]] instead to construct a ZLayer.
   */
  def apply[A, Ev](journal: Journal[Ev], reduce: Reducer[A, Ev]): EventSource[A, Ev] =
    new EventSourceLive(journal, reduce)

  final class EventSourceOf[A, Ev](implicit aTag: Tag[A], eTag: Tag[Ev]) {

    def persistNewAggregateFromEvent(event: Ev): ZIO[EventSource[A, Ev], Throwable, (AggregateId, A)] =
      ZIO.environmentWithZIO(_.get.persistNewAggregateFromEvent(event))

    def persist(uuid: AggregateId)(event: Ev): ZIO[EventSource[A, Ev], Throwable, A] =
      ZIO.environmentWithZIO(_.get.persist(uuid)(event))

    def persistEither(uuid: AggregateId)(command: A => Either[Throwable, Ev]): ZIO[EventSource[A, Ev], Throwable, A] =
      ZIO.environmentWithZIO(_.get.persistEither(uuid)(command))

    def read(uuid: AggregateId): ZIO[EventSource[A, Ev], Throwable, A] =
      ZIO.environmentWithZIO(_.get.read(uuid))

    def readOption(uuid: AggregateId): ZIO[EventSource[A, Ev], Throwable, Option[A]] =
      ZIO.environmentWithZIO(_.get.readOption(uuid))

    def readAll: ZStream[EventSource[A, Ev], Throwable, (AggregateId, A)] =
      ZStream.environmentWithStream(_.get.readAll)

    def events(uuid: AggregateId): ZStream[EventSource[A, Ev], Throwable, Ev] =
      ZStream.environmentWithStream(_.get.events(uuid))

  }

}

final class EventSourceLive[A, Ev] private[parana] (journal: Journal[Ev], reduce: Reducer[A, Ev])
    extends EventSource[A, Ev] {

  override def persistNewAggregateFromEvent(event: Ev): Task[(AggregateId, A)] =
    for {
      a    <- ZIO.fromEither(reduce(None, event))
      uuid <- ZIO.attempt(UUID.randomUUID()) // TODO ZIO random uuid
      _    <- journal.write(uuid, event)
    } yield uuid -> a

  override def persist(uuid: AggregateId)(event: Ev): Task[A] =
    for {
      maybeA <- readOption(uuid)
      newA   <- ZIO.fromEither(reduce(maybeA, event))
      _      <- journal.write(uuid, event).when(!(maybeA contains newA))
    } yield newA

  override def persistEither(uuid: AggregateId)(command: A => Either[Throwable, Ev]): Task[A] =
    for {
      maybeA <- readOption(uuid)
      event  <- maybeA.fold(noSuchElement(uuid))(a => ZIO.fromEither(command(a)))
      newA   <- ZIO.fromEither(reduce(maybeA, event))
      _      <- journal.write(uuid, event).when(!(maybeA contains newA))
    } yield newA

  private def noSuchElement(uuid: AggregateId): IO[Throwable, Ev] =
    ZIO.fail(new NoSuchElementException(s"$uuid"))

  override def read(uuid: AggregateId): Task[A] =
    readOption(uuid).someOrFail(new NoSuchElementException(s"$uuid"))

  override def readOption(uuid: AggregateId): Task[Option[A]] =
    journal
      .read(uuid)
      .runFold(zero)(reduceState)
      .flatMap(ZIO.fromEither(_))

  override def readAll: Stream[Throwable, (AggregateId, A)] =
    journal.all.map { case (uuid, events) => events.foldLeft(zero)(reduceState).map(uuid -> _) }
      .mapZIO(ZIO.fromEither(_))
      .collect { case (uuid, Some(a)) => uuid -> a }

  override def events(uuid: AggregateId): Stream[Throwable, Ev] = journal.read(uuid)

  private val zero: Either[Throwable, Option[A]] = Right(None)

  private def reduceState(prev: AggregateState, event: Ev): AggregateState =
    prev
      .flatMap(reduce(_, event))
      .map(Some(_))

  private type AggregateState = Either[Throwable, Option[A]]

}
