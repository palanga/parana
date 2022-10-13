package palanga.examples.reservations

import palanga.examples.reservations.commands.*
import palanga.examples.reservations.events.*
import palanga.examples.reservations.eventsourcing.*
import palanga.examples.reservations.model.*
import palanga.parana.eventsource.*
import palanga.parana.eventsource.local.*
import palanga.parana.journal.*
import zio.*

import java.time.LocalDate

object model:

  trait ReservationsManager:
    def reservationsByName(onBehalfOf: String): Task[Map[EntityId, Reservation]]
    def reservationsByDate(date: LocalDate): Task[Map[EntityId, Reservation]]
    def takeReservation(reservationId: EntityId): Task[Reservation]
    def concludeReservation(reservationId: EntityId): Task[Reservation]
    def cancelReservation(reservationId: EntityId): Task[Reservation]
    def makeReservation(onBehalfOf: String, date: LocalDate, shift: Shift): Task[(EntityId, Reservation)]

  case class Reservation(onBehalfOf: String, date: LocalDate, shift: Shift, status: Status = Status.Open):

    def take =
      status match
        case Status.Open         => Right(copy(status = Status.Taken))
        case Status.Taken        => Right(this)
        case Status.Concluded    => Left(Exception("Cannot take a reservation that is already concluded."))
        case Status.Cancelled(_) => Left(Exception("Cannot take a reservation that is already cancelled."))

    def conclude =
      status match
        case Status.Open | Status.Taken => Right(copy(status = Status.Concluded))
        case Status.Concluded           => Right(this)
        case Status.Cancelled(_)        => Left(Exception("Cannot conclude a reservation that is already cancelled."))

    def cancel(reason: String) =
      status match
        case Status.Open         => Right(copy(status = Status.Cancelled(reason)))
        case Status.Taken        => Left(Exception("Cannot cancel a reservation that has been taken."))
        case Status.Concluded    => Left(Exception("Cannot cancel a reservation that is already concluded."))
        case Status.Cancelled(_) => Right(this)

  enum Shift:
    case EightToTen, TenToTwelve

  enum Status:
    case Open, Taken, Concluded
    case Cancelled(reason: String)
    
  trait ReservationsIndex:
    def insert(id: EntityId, event: Event): Task[Unit]
    def searchByName(onBehalfOf: String): Task[Map[EntityId, Reservation]]
    def searchByDate(date: LocalDate, shift: Shift): Task[Map[EntityId, Reservation]]
    def countOccupiedByDate(date: LocalDate, shift: Shift): Task[Int]






