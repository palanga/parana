package palanga.parana

import palanga.parana.EventSource.EventSource
import palanga.parana.events.{ painters, PainterEvent }
import palanga.parana.model.Painter
import zio.ZIO
import zio.test.*
import zio.test.Assertion.*

import java.util.UUID

object EventSourceSpec {

  val testSuite =
    suite("an event source")(
      test("can persist a new aggregate from a creation event") {
        painters
          .persistNewAggregateFromEvent(PainterEvent.Born("Remedios Varo"))
          .map(_._2)
          .map(assert(_)(equalTo(Painter("Remedios Varo"))))
      },
      test("can't persist a new aggregate from a non creation event") {
        painters
          .persistNewAggregateFromEvent(PainterEvent.PaintingPainted("Bordando el manto terrestre"))
          .exit
          .map(assert(_)(fails(anything))) // TODO better error types
      },
      test("can read a persisted aggregate") {
        for {
          (uuid, _)     <- painters.persistNewAggregateFromEvent(PainterEvent.Born("Dorothea Tanning"))
          dorothea      <- painters.read(uuid)
          maybeDorothea <- painters.readOption(uuid)
        } yield assert(dorothea)(equalTo(Painter("Dorothea Tanning"))) &&
          assert(maybeDorothea)(isSome(equalTo(dorothea)))
      },
      test("can't read a non existent aggregate") {
        val uuid = UUID.randomUUID()
        for {
          readFailed <- painters.read(uuid).exit
          none       <- painters.readOption(uuid)
        } yield assert(readFailed)(fails(anything)) && // TODO better errors
          assert(none)(isNone)
      },
      test("can persist an event for an existent aggregate") {
        for {
          (uuid, _) <- painters.persistNewAggregateFromEvent(PainterEvent.Born("Tarsila do Amaral"))
          tarsila   <- painters.persist(uuid)(PainterEvent.PaintingPainted("Abaporu"))
        } yield assert(tarsila)(equalTo(Painter("Tarsila do Amaral", Set("Abaporu"))))
      },
      test("can't persist an event for a non existent aggregate") {
        painters
          .persist(UUID.randomUUID())(PainterEvent.PaintingPainted("Bordando el manto terrestre")) // TODO repetido
          .exit
          .map(assert(_)(fails(anything)))                                                         // TODO better error types
      },
      test("TODO - can't persist an event that can't be applied")(assertCompletes),
      test("TODO - can't persist a failed effectul event")(assertCompletes),
      test("can read all aggregates") { // TODO gen
        val frida  = Painter("Frida Kahlo", Set("Autorretrato con collar de espinas y colibrí", "La columna rota"))
        val berthe = Painter("Berthe Morisot", Set("Le berceau", "Femme à sa toilette"))
        for {
          (fridaId, _)  <- painters.persistNewAggregateFromEvent(PainterEvent.Born(frida.name))
          _             <- ZIO.foreach(frida.paintings.map(PainterEvent.PaintingPainted.apply))(painters.persist(fridaId))
          (bertheId, _) <- painters.persistNewAggregateFromEvent(PainterEvent.Born(berthe.name))
          _             <- ZIO.foreach(berthe.paintings.map(PainterEvent.PaintingPainted.apply))(painters.persist(bertheId))
          all           <- painters.readAll.runCollect
        } yield assert(all)(hasSubset(Set(fridaId -> frida, bertheId -> berthe)))
      },
    )

}
