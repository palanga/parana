//package palanga.parana
//
//import palanga.parana.events.{ reduce, PainterEvent }
//import zio.*
//import zio.test.*
//
//object CoreSpec extends ZIOSpecDefault {
//
//  override def spec =
//    testSuite.provideCustomLayerShared(dependencies) @@ TestAspect.parallelN(4)
//
//  private val testSuite =
//    suite("río paraná suite with in memory journal")(
//      EventSourceSpec.testSuite,
//      JournalSpec.testSuite,
//    )
//
//  private val dependencies =
//    (ZLayer.apply(Journal.inMemory[PainterEvent]) >>> EventSource.makeLayer(reduce)) ++ ZLayer.apply(
//      Journal.inMemory[PainterEvent]
//    )
//
//}
