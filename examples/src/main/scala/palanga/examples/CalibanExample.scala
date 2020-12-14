package palanga.examples

import caliban.GraphQL.graphQL
import caliban.schema.GenericSchema
import caliban.{ GraphQL, RootResolver }
import palanga.caliban.http4s.server
import palanga.examples.SimpleExample._
import palanga.zio.eventsourcing.EventSource.EventSource
import palanga.zio.eventsourcing.{ EventSource, Journal }
import zio.stream.ZStream
import zio.{ ExitCode, URIO, ZEnv, ZIO }

import java.util.UUID

object CalibanExample extends zio.App {

  case class Queries(
    painter: PainterArgs => ZIO[EventSource[Painter, Event], Throwable, Option[Painter]],
    painters: ZStream[EventSource[Painter, Event], Throwable, Painter],
    events: PainterArgs => ZStream[EventSource[Painter, Event], Throwable, Event],
  )

  case class Mutations(
    createPainter: CreatePainterArgs => ZIO[EventSource[Painter, Event], Throwable, (UUID, Event)],
    addPaintings: AddPaintingsArgs => ZIO[EventSource[Painter, Event], Throwable, Option[UUID]],
  )

  case class PainterArgs(id: UUID)
  case class CreatePainterArgs(name: Name, paintings: List[Painting])
  case class AddPaintingsArgs(id: UUID, paintings: List[Painting])

  private val painters = EventSource.of[Painter, Event]

  private val queries =
    Queries(
      painters read _.id,
      painters.readAll,
      painters events _.id,
    )

  private val mutations =
    Mutations(
      args => painters.write(Painter(args.name, args.paintings.toSet)),
      args =>
        painters
          .readAndApplyCommand(args.id, _ addPaintings args.paintings.toSet)
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
    server.run(ExampleApi.api).provideCustomLayer(fullLayer).exitCode

  private val fullLayer = Journal.inMemory[Event] >>> EventSource.live(applyEvent)

}
