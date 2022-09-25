package palanga.parana

import palanga.parana.model.{ Name, Painter, Painting }

object events {

  sealed trait PainterEvent
  object PainterEvent {
    case class Born(name: Name)                    extends PainterEvent
    case class PaintingPainted(painting: Painting) extends PainterEvent
  }

  def reduce: (Option[Painter], PainterEvent) => Either[Throwable, Painter] = {
    case (None, PainterEvent.Born(name))                         => Right(Painter(name))
    case (Some(painter), PainterEvent.PaintingPainted(painting)) => Right(painter.paint(painting))
    case (maybePainter, event)                                   => Left(new Exception(s"Cannot apply event <<$event>> to <<$maybePainter>>"))
  }

  val painters = EventSource.service[Painter, PainterEvent]

}
