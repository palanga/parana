package palanga.parana

import palanga.parana.types.*
import zio.*
import zio.stream.*

import java.util.{ NoSuchElementException, UUID }

type Reducer[A, Ev] = (Option[A], Ev) => Either[Throwable, A]

trait EventSource[A, Ev]:
  def persistNewAggregateFromEvent(event: Ev): Task[(AggregateId, A)]
  def persist(uuid: AggregateId)(event: Ev): Task[A]
  def persistEither(uuid: AggregateId)(command: A => Either[Throwable, Ev]): Task[A]
  def read(uuid: AggregateId): Task[A]
  def readOption(uuid: AggregateId): Task[Option[A]]
  def readAll: Stream[Throwable, (AggregateId, A)]
  def events(uuid: AggregateId): Stream[Throwable, Ev]

object EventSource:

  def make[A, Ev](reducer: Reducer[A, Ev])(implicit tag: Tag[Ev]): ZIO[Journal[Ev], Nothing, EventSource[A, Ev]] =
    for journal <- ZIO.service[Journal[Ev]]
    yield new EventSourceWithJournal(journal, reducer)

  def makeLayer[A, Ev](
    reducer: Reducer[A, Ev]
  )(implicit aTag: Tag[A], eTag: Tag[Ev]): ZLayer[Journal[Ev], Nothing, EventSource[A, Ev]] =
    ZLayer.fromZIO(make(reducer))

  /**
   * Get accessor methods for a specific event source
   */
  def service[A, Ev](implicit aTag: Tag[A], eTag: Tag[Ev]): EventSourceService[A, Ev] = EventSourceService[A, Ev]()

  final class EventSourceService[A, Ev](implicit aTag: Tag[A], eTag: Tag[Ev]):

    def persistNewAggregateFromEvent(event: Ev): ZIO[EventSource[A, Ev], Throwable, (AggregateId, A)] =
      ZIO.serviceWithZIO(_.persistNewAggregateFromEvent(event))

    def persist(uuid: AggregateId)(event: Ev): ZIO[EventSource[A, Ev], Throwable, A] =
      ZIO.serviceWithZIO(_.persist(uuid)(event))

    def persistEither(uuid: AggregateId)(command: A => Either[Throwable, Ev]): ZIO[EventSource[A, Ev], Throwable, A] =
      ZIO.serviceWithZIO(_.persistEither(uuid)(command))

    def read(uuid: AggregateId): ZIO[EventSource[A, Ev], Throwable, A] =
      ZIO.serviceWithZIO(_.read(uuid))

    def readOption(uuid: AggregateId): ZIO[EventSource[A, Ev], Throwable, Option[A]] =
      ZIO.serviceWithZIO(_.readOption(uuid))

    def readAll: ZStream[EventSource[A, Ev], Throwable, (AggregateId, A)] =
      ZStream.serviceWithStream(_.readAll)

    def events(uuid: AggregateId): ZStream[EventSource[A, Ev], Throwable, Ev] =
      ZStream.serviceWithStream(_.events(uuid))

final class EventSourceWithJournal[A, Ev] private[parana] (journal: Journal[Ev], reduce: Reducer[A, Ev])
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
