package palanga.examples.reservations

import palanga.examples.reservations.model.*

import java.time.LocalDate

object commands:
  enum Command:
    case MakeReservation(onBehalfOf: String, date: LocalDate, shift: Shift)
    case Take
    case Conclude
    case Cancel(reason: String)




