package palanga.examples
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import palanga.caliban.http4s.server
import palanga.examples.SimpleExample.{ applyEvent, Event }
import palanga.zio.cassandra.ZCqlSession
import palanga.zio.eventsourcing.{ journal, EventSource }
import palanga.zio.eventsourcing.journal.cassandra.CassandraJournal.Codec
import zio.{ ExitCode, URIO, ZEnv }

object CassandraCalibanExample extends zio.App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    server.run(CalibanExample.ExampleApi.api).provideCustomLayer(fullLayer).exitCode

  implicit private val codec = Codec[Event](_.asJson.noSpaces, decode[Event])

  private val fullLayer = ZCqlSession.layer.default >>> journal.cassandra.test[Event] >>> EventSource.live(applyEvent)

}
