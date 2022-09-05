package palanga.parana

import palanga.parana.events.{ reduce, PainterEvent }
import palanga.parana.journal.JournalSpec
import zio.*
import zio.test.*

object CoreSpec extends ZIOSpecDefault {

  override def spec =
    testSuite.provideCustomLayerShared(dependencies) @@ TestAspect.parallelN(4)

  private val testSuite =
    suite("río paraná suite with in memory journal")(
      EventSourceSpec.testSuite,
      JournalSpec.testSuite,
    )

  private val dependencies =
    (ZLayer.apply(journal.inMemory[PainterEvent]) >>> EventSource.live(reduce)) ++ ZLayer.apply(
      journal.inMemory[PainterEvent]
    )

}
