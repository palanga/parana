package palanga.examples

import palanga.parana.EventSource.EventSource
import palanga.parana.*
import zio.*
import zio.json.*

import java.util.UUID

object SimpleExample {

  // We will model painters and paintings.
  case class Painter(name: Name, paintings: Set[Painting]) {
    def addPaintings(paintings: Set[Painting]): Painter = copy(paintings = this.paintings ++ paintings)
  }

  sealed trait PainterEvent
  object PainterEvent {
    case class Created(name: Name, paintings: Set[Painting]) extends PainterEvent
    case class PaintingsAdded(paintings: Set[Painting])      extends PainterEvent
  }

  // A function that is used to recover a painter from events.
  // It takes an optional painter and an event, and returns either
  // an error or the painter after the applied event.
  // Note that in the case of painter absence (None value) means that
  // the painter is not already created, so in that case, the only
  // possible event is the `Created` one.
  def reduce: (Option[Painter], PainterEvent) => Either[Throwable, Painter] = {
    case (None, PainterEvent.Created(name, paintings))           => Right(Painter(name, paintings))
    case (Some(painter), PainterEvent.PaintingsAdded(paintings)) => Right(painter.addPaintings(paintings))
    case (maybePainter, event)                                   => Left(new Exception(s"$maybePainter $event"))
  }

  // Create an event source for our types.
  val painters = EventSource.of[Painter, PainterEvent]

  // Then we can use EventSource methods like this (there are more).
  def createPainter(
    name: Name,
    paintings: Set[Painting] = Set.empty,
  ): ZIO[EventSource[Painter, PainterEvent], Throwable, (AggregateId, Painter)] =
    painters.persistNewAggregateFromEvent(PainterEvent.Created(name, paintings))

  def getPainter(uuid: UUID): ZIO[EventSource[Painter, PainterEvent], Throwable, Painter] =
    painters.read(uuid)

  def addPaintings(uuid: UUID, paintings: Set[Painting]): ZIO[EventSource[Painter, PainterEvent], Throwable, Painter] =
    painters.persist(uuid)(PainterEvent.PaintingsAdded(paintings))

  // Create our dependencies.
  val inMemoryLayer = ZLayer.apply(journal.inMemory[PainterEvent]) >>> EventSource.live(reduce)

  // If you want to use a cassandra journal instead you can:
  given JsonCodec[PainterEvent] = DeriveJsonCodec.gen[PainterEvent]
  val cassandraLayer            = journal.cassandra.json.live[PainterEvent] >>> EventSource.live(reduce)

  type Name     = String
  type Painting = String

}
