package palanga.parana

import palanga.parana.events.{ reduce, PainterEvent }
import palanga.parana.journal.JournalSpec
import zio.test.{ DefaultRunnableSpec, TestAspect }

object CoreSpec extends DefaultRunnableSpec {

  override def spec =
    testSuite.provideCustomLayerShared(dependencies) @@ TestAspect.parallelN(4)

  private val testSuite =
    suite("río paraná suite with in memory journal")(
      EventSourceSpec.testSuite,
      JournalSpec.testSuite,
    )

  private val dependencies =
    (journal.inMemory[PainterEvent].toLayer >>> EventSource.live(reduce)) ++ journal.inMemory[PainterEvent].toLayer

}
