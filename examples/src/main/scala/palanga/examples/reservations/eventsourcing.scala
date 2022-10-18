package palanga.examples.reservations

import palanga.examples.reservations.commands.*
import palanga.examples.reservations.events.*
import palanga.examples.reservations.model.*
import palanga.parana.eventsource.*
import palanga.parana.eventsource.local.*
import palanga.parana.journal.*
import zio.*
import zio.stm.*
import zio.stm.ZSTM.ifSTM

import java.time.LocalDate

object eventsourcing:

  final class EventSourcedReservationsManager(
    reservations: EventSource[Reservation, Command, Event],
    index: ReservationsIndex,
  ) extends ReservationsManager:

    override def reservationsByName(onBehalfOf: String): Task[Map[EntityId, Reservation]] =
      index.searchByName(onBehalfOf)

    override def reservationsByDate(date: LocalDate): Task[Map[EntityId, Reservation]] =
      index.searchByDate(date, Shift.EightToTen).zipPar(index.searchByDate(date, Shift.TenToTwelve)).map(_ ++ _)

    override def takeReservation(reservationId: EntityId): Task[Reservation] =
      reservations.of(reservationId).ask(Command.Take).map(_._1)

    override def concludeReservation(reservationId: EntityId): Task[Reservation] =
      reservations.of(reservationId).ask(Command.Conclude).map(_._1)

    override def cancelReservation(reservationId: EntityId): Task[Reservation] =
      reservations.of(reservationId).ask(Command.Cancel("No reason")).map(_._1)

    override private[reservations] def saveReservation(reservation: Reservation): Task[(EntityId, Reservation)] =
      reservations.empty.ask(Command.MakeReservation(reservation)).map(_._1)

    override private[reservations] def countOccupiedTables(date: LocalDate, shift: Shift): Task[Int] =
      index.countOccupiedByDate(date, shift)

    override private[reservations] val amountOfTables: Int = 2

  object EventSourcedReservationsManager:

    def makeLayer = ZLayer.fromZIO(make)

    def make: ZIO[ReservationsIndex & EventSource[Reservation, Command, Event], Nothing, ReservationsManager] =
      for
        reservations <- ZIO.service[EventSource[Reservation, Command, Event]]
        index        <- ZIO.service[ReservationsIndex]
      yield EventSourcedReservationsManager(reservations, index)

  private val initCommand: PartialFunction[Command, (Reservation, List[Event])] = {
    case Command.MakeReservation(reservation) =>
      reservation -> List(Event.ReservationMade(reservation.onBehalfOf, reservation.date, reservation.shift))
  }

  private val applyCommand: (Reservation, Command) => Task[(Reservation, List[Event])] = (reservation, command) =>
    command match
      case _: Command.MakeReservation =>
        ZIO.fail(Exception("Reservation already made"))
      case Command.Take               =>
        ZIO.fromEither(reservation.take.map(_ -> List(Event.Taken)))
      case Command.Conclude           =>
        ZIO.fromEither(reservation.conclude.map(_ -> List(Event.Concluded)))
      case Command.Cancel(reason)     =>
        ZIO.fromEither(reservation.cancel(reason).map(_ -> List(Event.Cancelled(reason))))

  private val initEvent: PartialFunction[Event, Reservation] = { case Event.ReservationMade(onBehalfOf, date, shift) =>
    Reservation(onBehalfOf, date, shift)
  }

  private val applyEvent: (Reservation, Event) => Either[Throwable, Reservation] =
    (reservation, event) =>
      event match
        case Event.ReservationMade(onBehalfOf, date, shift) => Right(Reservation(onBehalfOf, date, shift))
        case Event.Taken                                    => reservation.take
        case Event.Concluded                                => reservation.conclude
        case Event.Cancelled(reason)                        => reservation.cancel(reason)

  val reservationsLocal =
    EventSourceLocal
      .of(initCommand, applyCommand, initEvent, applyEvent)
      .withProjection(indexProjection)
      .makeLayer

  def indexProjection(id: EntityId, event: Event): ZIO[ReservationsIndex, Nothing, Unit] =
    for
      index <- ZIO.service[ReservationsIndex]
      _     <- index.insert(id, event).ignoreLogged
    yield ()

  final class InMemoryReservationsIndex(
    initEvent: PartialFunction[Event, Reservation],
    applyEvent: (Reservation, Event) => Either[Throwable, Reservation],
    reservationsById: TMap[EntityId, Reservation],
    reservationsByName: TMap[String, Map[EntityId, Reservation]],
    reservationsByDate: TMap[LocalDate, Map[EntityId, Reservation]],
  ) extends ReservationsIndex:

    override def insert(id: EntityId, event: Event): Task[Unit] =
      ifSTM(reservationsById.contains(id))(getAndApply(id, event), initiateOrFail(event))
        .flatMap(updateIndices(id, _))
        .commit

    private def getAndApply(id: EntityId, event: Event): TaskSTM[Reservation] =
      reservationsById.get(id).someOrFailException.map(applyEvent(_, event)).absolve

    private def initiateOrFail(event: Event): TaskSTM[Reservation] =
      if initEvent.isDefinedAt(event)
      then STM.succeed(initEvent(event))
      else STM.fail(Exception(s"Cannot init Reservation from an $event event."))

    private def updateIndices(id: EntityId, reservation: Reservation): TaskSTM[Unit] =
      for
        _ <- reservationsById.put(id, reservation)
        _ <- reservationsByName.merge(reservation.onBehalfOf, Map(id -> reservation))(_ ++ _)
        _ <- reservationsByDate.merge(reservation.date, Map(id -> reservation))(_ ++ _)
      yield ()

    override def searchByName(onBehalfOf: String): Task[Map[EntityId, Reservation]] =
      reservationsByName.getOrElse(onBehalfOf, Map.empty).commit

    override def searchByDate(date: LocalDate, shift: Shift): Task[Map[EntityId, Reservation]] =
      reservationsByDate.getOrElse(date, Map.empty).commit.map(_.filter(_._2.shift == shift))

    override def countOccupiedByDate(date: LocalDate, shift: Shift): Task[Int] =
      searchByDate(date, shift)
        .map(_.map(_._2.status))
        .map(_.filter(status => status == Status.Open || status == Status.Taken))
        .map(_.size)

  object InMemoryReservationsIndex:

    def makeLayer = ZLayer.fromZIO(make.commit)

    def make: ZSTM[Any, Nothing, ReservationsIndex] =
      for
        reservationsById   <- TMap.empty[EntityId, Reservation]
        reservationsByName <- TMap.empty[String, Map[EntityId, Reservation]]
        reservationsByDate <- TMap.empty[LocalDate, Map[EntityId, Reservation]]
      yield InMemoryReservationsIndex(initEvent, applyEvent, reservationsById, reservationsByName, reservationsByDate)

  val inMemoryJournal = InMemoryJournal.makeLayer[Event]
