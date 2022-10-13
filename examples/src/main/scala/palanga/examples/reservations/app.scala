package palanga.examples.reservations

import palanga.examples.reservations.commands.*
import palanga.examples.reservations.events.*
import palanga.examples.reservations.eventsourcing.*
import palanga.examples.reservations.model.*
import zio.*

import java.time.LocalDate

object app extends ZIOAppDefault:

  override def run =
    app.provide(
      EventSourcedReservationsManager.makeLayer,
      reservationsLocal,
      inMemoryJournal,
      InMemoryReservationsIndex.makeLayer,
    )

  private val today    = LocalDate.now()
  private val tomorrow = today.plusDays(1)

  private val app =
    for
      reservationsManager <- ZIO.service[ReservationsManager]
      (idPalan, _)        <- reservationsManager.makeReservation("Palan", tomorrow, Shift.EightToTen).debug
      (idNube, _)         <- reservationsManager.makeReservation("Nube", tomorrow, Shift.EightToTen).debug
      _                   <- reservationsManager.makeReservation("Fruchi", tomorrow, Shift.EightToTen).debug.ignore
      _                   <- reservationsManager.takeReservation(idPalan).debug
      _                   <- reservationsManager.cancelReservation(idNube).debug
      _                   <- reservationsManager.reservationsByDate(tomorrow).debug
      _                   <- reservationsManager.reservationsByName("Nube").debug
    yield ()
