package palanga.parana.journal

import palanga.parana.events.PainterEvent
import palanga.parana.{ journal, AggregateId }
import zio.test.Assertion.*
import zio.test.*
import zio.{ Queue, ZIO, ZLayer }

import java.util.UUID

object JournalSpec {

  val testSuite =
    suite("a journal")(
      test("can have a projection that require dependencies") {

        val journalDecorator =
          journal
            .decorator[PainterEvent]
            .tap(projectToQueue)
            .toLayer

        val id    = UUID.randomUUID()
        val event = PainterEvent.Born("Tarsila do Amaral")

        (for {
          _        <- ZIO.environmentWithZIO[Journal[PainterEvent]](_.get.write(id, event))
          dequeued <- ZIO.environmentWithZIO[Queue[(AggregateId, PainterEvent)]](_.get.take)
        } yield assert(dequeued)(equalTo(id -> event)))
          .provideSomeLayer(journalDecorator)

      },
      test("can have multiple sequential projections") {

        val journalDecorator =
          journal
            .decorator[PainterEvent]
            .tap(projectToQueue)
            .tap(projectToStringQueue)
            .toLayer

        val id    = UUID.randomUUID()
        val event = PainterEvent.Born("Remedios Varo")

        (for {
          _              <- ZIO.environmentWithZIO[Journal[PainterEvent]](_.get.write(id, event))
          dequeuedEvent  <- ZIO.environmentWithZIO[Queue[(AggregateId, PainterEvent)]](_.get.take)
          dequeuedString <- ZIO.environmentWithZIO[Queue[String]](_.get.take)
        } yield assert(dequeuedEvent)(equalTo(id -> event)) && assert(dequeuedString)(equalTo(event.toString)))
          .provideSomeLayer(journalDecorator)

      },
      test("can have multiple parallel projections") {

        val journalDecorator =
          journal
            .decorator[PainterEvent]
            .tap(projectToQueue)
            .tapPar(projectToStringQueue)
            .toLayer

        val id    = UUID.randomUUID()
        val event = PainterEvent.Born("Frida Kahlo")

        (for {
          _              <- ZIO.serviceWithZIO[Journal[PainterEvent]](_.write(id, event))
          dequeuedEvent  <- ZIO.serviceWithZIO[Queue[(AggregateId, PainterEvent)]](_.take)
          dequeuedString <- ZIO.serviceWithZIO[Queue[String]](_.take)
        } yield assert(dequeuedEvent)(equalTo(id -> event)) && assert(dequeuedString)(equalTo(event.toString)))
          .provideSomeLayer(journalDecorator)

      },
      test("can be decorated") {

        val journalDecorator =
          journal
            .decorator[PainterEvent]
            .tap(projectToQueue)
            .tap(projectToStringQueue)

        val decoratedJournalInMemory = journalDecorator.decorate(journal.inMemory)

        val id    = UUID.randomUUID()
        val event = PainterEvent.Born("Dorothea Tanning")

        (for {
          _              <- ZIO.environmentWithZIO[Journal[PainterEvent]](_.get.write(id, event))
          dequeuedEvent  <- ZIO.environmentWithZIO[Queue[(AggregateId, PainterEvent)]](_.get.take)
          dequeuedString <- ZIO.environmentWithZIO[Queue[String]](_.get.take)
        } yield assert(dequeuedEvent)(equalTo(id -> event)) && assert(dequeuedString)(equalTo(event.toString)))
          .provideSomeLayer(ZLayer.apply(decoratedJournalInMemory))

      },
    ).provideSomeLayer[Journal[PainterEvent]](queuesLayer)

  private def projectToQueue(id: AggregateId, event: PainterEvent) =
    ZIO.environmentWithZIO[Queue[(AggregateId, PainterEvent)]](_.get.offer(id, event))

  private def projectToStringQueue(id: AggregateId, event: PainterEvent) =
    ZIO.environmentWithZIO[Queue[String]](_.get.offer(event.toString))

  private def queuesLayer =
    ZLayer.apply(Queue.unbounded[(AggregateId, PainterEvent)]) ++ ZLayer.apply(Queue.unbounded[String])

}
