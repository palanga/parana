package palanga.parana

import palanga.parana.events.PainterEvent
import zio.test.Assertion._
import zio.test._
import zio.{ Queue, ZQueue }

import java.util.UUID

object JournalSpec {

  val testSuite: Spec[Any, TestFailure[Throwable], TestSuccess] =
    suite("a journal")(
      testM("can have projections") {

        val id    = UUID.randomUUID()
        val event = PainterEvent.Born("Frida Kahlo")

        def projectToQueue(queue: Queue[(AggregateId, PainterEvent)])(id: AggregateId, event: PainterEvent) =
          queue.offer(id, event)

        for {
          queue    <- ZQueue.unbounded[(AggregateId, PainterEvent)]
          journal  <- journal.inMemory[PainterEvent].tap(projectToQueue(queue)).raw
          _        <- journal.write(id, event)
          dequeued <- queue.take
        } yield assert(dequeued)(equalTo(id -> event))

      }
    )

}
