package palanga.zio.eventsourcing

import palanga.zio.eventsourcing.EventSource.EventSource
import palanga.zio.eventsourcing.events.{ painters, PainterEvent }
import palanga.zio.eventsourcing.model.Painter
import zio.ZIO
import zio.test.Assertion._
import zio.test.{ TestFailure, _ }

import java.util.UUID

object EventSourceSpec {

  val testSuite: Spec[EventSource[Painter, PainterEvent], TestFailure[Throwable], TestSuccess] =
    suite("an event source")(
      testM("can persist a new aggregate from a creation event") {
        painters
          .persistNewAggregateFromEvent(PainterEvent.Born("Remedios Varo"))
          .map(_._2)
          .map(assert(_)(equalTo(Painter("Remedios Varo"))))
      },
      testM("can't persist a new aggregate from a non creation event") {
        painters
          .persistNewAggregateFromEvent(PainterEvent.PaintingPainted("Bordando el manto terrestre"))
          .run
          .map(assert(_)(fails(anything))) // TODO better error types
      },
      testM("can read a persisted aggregate") {
        for {
          (uuid, _)     <- painters.persistNewAggregateFromEvent(PainterEvent.Born("Dorothea Tanning"))
          dorothea      <- painters.read(uuid)
          maybeDorothea <- painters.readOption(uuid)
        } yield assert(dorothea)(equalTo(Painter("Dorothea Tanning"))) &&
          assert(maybeDorothea)(isSome(equalTo(dorothea)))
      },
      testM("can't read a non existent aggregate") {
        val uuid = UUID.randomUUID()
        for {
          readFailed <- painters.read(uuid).run
          none       <- painters.readOption(uuid)
        } yield assert(readFailed)(fails(anything)) && // TODO better errors
          assert(none)(isNone)
      },
      testM("can persist an event for an existent aggregate") {
        for {
          (uuid, _) <- painters.persistNewAggregateFromEvent(PainterEvent.Born("Tarsila do Amaral"))
          tarsila   <- painters.persist(uuid)(PainterEvent.PaintingPainted("Abaporu"))
        } yield assert(tarsila)(equalTo(Painter("Tarsila do Amaral", Set("Abaporu"))))
      },
      testM("can't persist an event for a non existent aggregate") {
        painters
          .persist(UUID.randomUUID())(PainterEvent.PaintingPainted("Bordando el manto terrestre")) // TODO repetido
          .run
          .map(assert(_)(fails(anything)))                                                         // TODO better error types
      },
      test("TODO - can't persist an event that can't be applied")(assertCompletes),
      test("TODO - can't persist a failed effectul event")(assertCompletes),
      testM("can read all aggregates") { // TODO gen
        val frida  = Painter("Frida Kahlo", Set("Autorretrato con collar de espinas y colibrí", "La columna rota"))
        val berthe = Painter("Berthe Morisot", Set("Le berceau", "Femme à sa toilette"))
        for {
          (fridaId, _)  <- painters.persistNewAggregateFromEvent(PainterEvent.Born(frida.name))
          _             <- ZIO.foreach(frida.paintings.map(PainterEvent.PaintingPainted))(painters.persist(fridaId))
          (bertheId, _) <- painters.persistNewAggregateFromEvent(PainterEvent.Born(berthe.name))
          _             <- ZIO.foreach(berthe.paintings.map(PainterEvent.PaintingPainted))(painters.persist(bertheId))
          all           <- painters.readAll.runCollect
        } yield assert(all)(hasSubset(Set(fridaId -> frida, bertheId -> berthe)))
      },
    )

}
