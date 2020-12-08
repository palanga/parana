package palanga.examples

import caliban.GraphQL.graphQL
import caliban.schema.GenericSchema
import caliban.{ GraphQL, Http4sAdapter, RootResolver }
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import palanga.examples.SimpleExample._
import palanga.zio.eventsourcing.EventSource.EventSource
import palanga.zio.eventsourcing.{ EventSource, Journal }
import zio._
import zio.interop.catz._
import zio.stream.ZStream

import java.util.UUID

object CalibanExample extends zio.App {

  case class Queries(
    painter: PainterArgs => ZIO[EventSource[Painter, Event], Throwable, Option[Painter]],
    events: PainterArgs => ZStream[EventSource[Painter, Event], Throwable, Event],
  )

  case class Mutations(
    createPainter: CreatePainterArgs => ZIO[EventSource[Painter, Event], Throwable, (UUID, Event)],
    addPaintings: AddPaintingsArgs => ZIO[EventSource[Painter, Event], Throwable, Option[UUID]],
  )

  case class PainterArgs(id: UUID)
  case class CreatePainterArgs(name: Name, paintings: List[Painting])
  case class AddPaintingsArgs(id: UUID, paintings: List[Painting])

  private val queries =
    Queries(
      EventSource read [Painter, Event] _.id,
      EventSource events [Painter, Event] _.id,
    )

  private val mutations =
    Mutations(
      args =>
        EventSource
          .write[Painter, Event](Painter(args.name, args.paintings.toSet)),
      args =>
        EventSource
          .readAndApplyCommand[Painter, Event](args.id, _ addPaintings args.paintings.toSet)
          .as(Some(args.id)),
    )

  object ExampleApi extends GenericSchema[EventSource[Painter, Event]] {
    val api: GraphQL[EventSource[Painter, Event]] =
      graphQL(
        RootResolver(
          queries,
          mutations,
        )
      )
  }

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    ZIO
      .runtime[ZEnv with EventSource[Painter, Event]]
      .flatMap(implicit runtime =>
        for {
          interpreter <- ExampleApi.api.interpreter
          _           <- BlazeServerBuilder[ExampleTask](runtime.platform.executor.asEC)
                 .bindHttp(8088, "localhost")
                 .withHttpApp(
                   Router[ExampleTask](
                     "/api/graphql" -> CORS(Http4sAdapter.makeHttpService(interpreter)),
                     "/ws/graphql"  -> CORS(Http4sAdapter.makeWebSocketService(interpreter)),
                   ).orNotFound
                 )
                 .resource
                 .toManaged
                 .useForever
        } yield ()
      )
      .provideCustomLayer(fullLayer)
      .exitCode

  private val fullLayer = Journal.inMemory[Event] >>> EventSource.live(applyEvent)

  private type ExampleTask[A] = RIO[ZEnv with EventSource[Painter, Event], A]

}
