package palanga.examples.reservations

import palanga.examples.reservations.model.*

import java.time.LocalDate


object events:
  enum Event:
    case ReservationMade(onBehalfOf: String, date: LocalDate, shift: Shift)
    case Taken
    case Concluded
    case Cancelled(reason: String)


