package palanga.parana

import palanga.parana.events.{ reduce, PainterEvent }
import zio.test.{ DefaultRunnableSpec, TestAspect }

object Spec extends DefaultRunnableSpec {

  override def spec =
    testSuite.provideCustomLayerShared(dependencies.orDie) @@ TestAspect.parallelN(4)

  private val testSuite =
    suite("río paraná suite")(
      EventSourceSpec.testSuite,
      JournalSpec.testSuite,
    )

  private val dependencies = journal.inMemory[PainterEvent].layer >>> EventSource.live(reduce)

}
