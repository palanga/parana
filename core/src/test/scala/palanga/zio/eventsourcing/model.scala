package palanga.zio.eventsourcing

object model {

  case class Painter private (name: Name, paintings: Set[Painting] = Set.empty) {

    def steal(painter: Painter)(painting: Painting): Either[Throwable, Painter] =
      for {
        paintingToSteal <- painter.paintings.find(_ == painting).toRight(new NoSuchElementException())
        thiefPainter    <- if (isThief) Right(copy(paintings = paintings + paintingToSteal))
                           else Left(new UnsupportedOperationException(s"$name is not a thief"))
      } yield thiefPainter

    def paint(paintings: Painting): Painter = copy(paintings = this.paintings + paintings)

    private def isThief = this.name.toLowerCase.contains("nik")

  }

  type Name     = String
  type Painting = String

}
