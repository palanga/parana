zio-event-sourcing
==================

An event sourcing library on top of ZIO
---------------------------------------

* Scala 2.13.4
* ZIO 1.0.3

Installation
------------

Add this to your `build.sbt` file
```sbt
resolvers += "Artifactory" at "https://palanga.jfrog.io/artifactory/maven/"
libraryDependencies += "dev.palanga" %% "zio-event-sourcing" % "version"
```

Usage
-----

```scala
import palanga.zio.eventsourcing.{ EventSource, Journal }

import java.util.UUID

object SimpleExample {

  // We will model painters and paintings.
  // Creating a painter returns either an error or a pair containing the
  // aggregate id and the event of painter created.
  object Painter {
    def apply(
      name: Name,
      paintings: Set[Painting] = Set.empty,
      id: UUID = UUID.randomUUID(),
    ): Either[Throwable, (UUID, Event.Created)] =
      if (name.isBlank) Left(new Exception(s"No name"))
      else Right(id -> Event.Created(Painter(paintings, name, id)))
  }

  // Adding paintings to a painter returns either an error or the event of
  // paintings added to the painter.
  case class Painter(paintings: Set[Painting], name: Name, id: UUID) {
    def addPaintings(paintings: Set[Painting]) =
      if (paintings.isEmpty) Left(new Exception(s"AddPaintings: empty paintings"))
      else Right(Event.PaintingsAdded(paintings))
  }

  // The mentioned events.
  sealed trait Event
  object Event {
    case class Created(painter: Painter)                extends Event
    case class PaintingsAdded(paintings: Set[Painting]) extends Event
  }

  // A function that is used to recover a painter from events.
  // It takes an optional painter and an event, and returns either
  // an error or the painter after the applied event.
  // Note that in the case of painter absence (None value) means that
  // the painter is not already created, so in that case, the only
  // possible event is the `Created` one.
  def applyEvent: (Option[Painter], Event) => Either[Throwable, Painter] = {
    case (None, Event.Created(painter))               => Right(painter)
    case (Some(painter), added: Event.PaintingsAdded) => Right(painter.copy(painter.paintings ++ added.paintings))
    case (maybePainter, event)                        => Left(new Exception(s"$maybePainter $event"))
  }

  // Create an event source for our types without the dependencies it needs.
  val painters = EventSource.of[Painter, Event]

  // Then we can use EventSource methods like this, leaving
  // the dependencies it needs for later.
  def createPainter(name: Name, paintings: Set[Painting] = Set.empty) =
    painters write Painter(name, paintings)

  def getPainter(id: UUID) =
    painters read id

  // Create our dependencies.
  val appLayer = Journal.inMemory[Event] >>> EventSource.live(applyEvent)

  // Providing `appLayer` eliminates all the dependencies.
  val painterIO = createPainter("Remedios Varo").provideLayer(appLayer)

  // Type aliases for convenience.
  type Name     = String
  type Painting = String

}

```

Testing:
--------

* To run tests: `./sbt` then `test`
