package palanga.examples

import palanga.caliban.http4s.server
import palanga.examples.SimpleExample.{ applyEvent, Event, Painter }
import palanga.zio.cassandra.session
import palanga.zio.eventsourcing.{ journal, EventSource }
import zio.json._
import zio.{ ExitCode, URIO, ZEnv }

import java.util.UUID

object CassandraCalibanExample extends zio.App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    server.run(CalibanExample.ExampleApi.api).provideCustomLayer(fullLayer).exitCode

  implicit val uuidEncoder = JsonEncoder.string.contramap[UUID](_.toString)
  implicit val uuidDecoder = JsonDecoder.string.map[UUID](UUID.fromString)

  implicit val painterEncoder = DeriveJsonEncoder.gen[Painter]
  implicit val painterDecoder = DeriveJsonDecoder.gen[Painter]

  implicit val eventEncoder = DeriveJsonEncoder.gen[Event]
  implicit val eventDecoder = DeriveJsonDecoder.gen[Event]

  private val fullLayer = session.layer.default >>> journal.cassandra.json.test[Event] >>> EventSource.live(applyEvent)

}
