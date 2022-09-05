package palanga.parana.journal.cassandra.json

import palanga.parana.events.{ reduce, PainterEvent }
import palanga.parana.{ journal, EventSource, EventSourceSpec }
import palanga.parana.journal.JournalSpec
import zio.Clock
import zio.Console
import zio.json.*
import zio.json.{ JsonDecoder, JsonEncoder }
import zio.test.*
import zio.*

object JournalCassandraJsonSpec extends ZIOSpecDefault {

  override def spec = testSuite.provideCustomLayerShared(dependencies.orDie)

  private val testSuite =
    suite("río paraná suite with cassandra journal and json codec")(
      EventSourceSpec.testSuite,
      JournalSpec.testSuite,
    )

  implicit val encoder: JsonEncoder[PainterEvent] = DeriveJsonEncoder.gen
  implicit val decoder: JsonDecoder[PainterEvent] = DeriveJsonDecoder.gen

  private val journalLayer =
    ZLayer.scoped(Live.live(palanga.zio.cassandra.session.auto.openDefault())) >>>
      journal.cassandra.json.test[PainterEvent]

  private val dependencies = (journalLayer >>> EventSource.live(reduce)) ++ journalLayer

}
