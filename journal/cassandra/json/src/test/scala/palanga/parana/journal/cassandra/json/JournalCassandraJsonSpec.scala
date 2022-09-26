//package palanga.parana.journal.cassandra.json
//
//import palanga.parana.eventsource.*
//import palanga.parana.journal
//import zio.*
//import zio.json.*
//import zio.test.*
//
//object JournalCassandraJsonSpec extends ZIOSpecDefault {
//
//  override def spec = testSuite.provideCustomLayerShared(dependencies.orDie)
//
//  private val testSuite =
//    suite("río paraná suite with cassandra journal and json codec")(
//      EventSourceSpec.testSuite,
//      JournalSpec.testSuite,
//    )
//
//  implicit val encoder: JsonEncoder[PainterEvent] = DeriveJsonEncoder.gen
//  implicit val decoder: JsonDecoder[PainterEvent] = DeriveJsonDecoder.gen
//
//  private val journalLayer =
//    ZLayer.scoped(Live.live(palanga.zio.cassandra.session.auto.openDefault())) >>>
//      journal.cassandra.json.test[PainterEvent]
//
//  private val dependencies = (journalLayer >>> EventSource.makeLayer(reduce)) ++ journalLayer
//
//}
