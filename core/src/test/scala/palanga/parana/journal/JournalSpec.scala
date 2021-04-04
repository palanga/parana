package palanga.parana.journal

import palanga.parana.events.PainterEvent
import palanga.parana.{ journal, AggregateId }
import zio.test.Assertion._
import zio.test._
import zio.{ Has, Queue, ZIO }

import java.util.UUID

object JournalSpec {

  val testSuite =
    suite("a journal")(
      testM("can have a projection that require dependencies") {

        val journalDecorator =
          journal
            .decorator[PainterEvent]
            .tap(projectToQueue)
            .toLayer

        val id    = UUID.randomUUID()
        val event = PainterEvent.Born("Tarsila do Amaral")

        (for {
          _        <- ZIO.accessM[Journal[PainterEvent]](_.get.write(id, event))
          dequeued <- ZIO.accessM[Has[Queue[(AggregateId, PainterEvent)]]](_.get.take)
        } yield assert(dequeued)(equalTo(id -> event)))
          .provideSomeLayer(journalDecorator)

      },
      testM("can have multiple sequential projections") {

        val journalDecorator =
          journal
            .decorator[PainterEvent]
            .tap(projectToQueue)
            .tap(projectToStringQueue)
            .toLayer

        val id    = UUID.randomUUID()
        val event = PainterEvent.Born("Remedios Varo")

        (for {
          _              <- ZIO.accessM[Journal[PainterEvent]](_.get.write(id, event))
          dequeuedEvent  <- ZIO.accessM[Has[Queue[(AggregateId, PainterEvent)]]](_.get.take)
          dequeuedString <- ZIO.accessM[Has[Queue[String]]](_.get.take)
        } yield assert(dequeuedEvent)(equalTo(id -> event)) && assert(dequeuedString)(equalTo(event.toString)))
          .provideSomeLayer(journalDecorator)

      },
      testM("can have multiple parallel projections") {

        val journalDecorator =
          journal
            .decorator[PainterEvent]
            .tap(projectToQueue)
            .tapPar(projectToStringQueue)
            .toLayer

        val id    = UUID.randomUUID()
        val event = PainterEvent.Born("Remedios Varo")

        (for {
          _              <- ZIO.accessM[Journal[PainterEvent]](_.get.write(id, event))
          dequeuedEvent  <- ZIO.accessM[Has[Queue[(AggregateId, PainterEvent)]]](_.get.take)
          dequeuedString <- ZIO.accessM[Has[Queue[String]]](_.get.take)
        } yield assert(dequeuedEvent)(equalTo(id -> event)) && assert(dequeuedString)(equalTo(event.toString)))
          .provideSomeLayer(journalDecorator)

      },
    ).provideSomeLayer[Journal[PainterEvent]](queuesLayer)

  private def projectToQueue(id: AggregateId, event: PainterEvent) =
    ZIO.accessM[Has[Queue[(AggregateId, PainterEvent)]]](_.get.offer(id, event))

  private def projectToStringQueue(id: AggregateId, event: PainterEvent) =
    ZIO.accessM[Has[Queue[String]]](_.get.offer(event.toString))

  private def queuesLayer = Queue.unbounded[(AggregateId, PainterEvent)].toLayer ++ Queue.unbounded[String].toLayer

}
