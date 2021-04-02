package palanga.zio.eventsourcing

import palanga.zio.eventsourcing.events.{ reduce, PainterEvent }
import zio.test.{ DefaultRunnableSpec, TestAspect, _ }

object Spec extends DefaultRunnableSpec {

  override def spec =
    testSuite.provideCustomLayerShared(dependencies.orDie) @@ TestAspect.parallelN(4)

  private val testSuite =
    suite("zio event sourcing suite")(
      EventSourceSpec.testSuite,
      JournalSpec.testSuite,
    )

  private val dependencies = journal.inMemory[PainterEvent].layer >>> EventSource.live(reduce)

}
