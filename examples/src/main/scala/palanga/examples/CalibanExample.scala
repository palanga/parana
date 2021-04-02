package palanga.examples

import caliban.GraphQL.graphQL
import caliban.schema.GenericSchema
import caliban.{ GraphQL, RootResolver }
import palanga.caliban.http4s.server
import palanga.examples.SimpleExample._
import palanga.zio.eventsourcing.EventSource.EventSource
import palanga.zio.eventsourcing.{ journal, AggregateId, EventSource }
import zio.stream.ZStream
import zio.{ ExitCode, URIO, ZEnv, ZIO }

import java.util.UUID

/**
 * Head over to [[SimpleExample]] to see the full picture
 */
object CalibanExample extends zio.App {

  override def run(args: List[String]): URIO[ZEnv, ExitCode] =
    server.run(PaintersApi.graphql).provideCustomLayer(fullLayer).exitCode

  case class Queries(
    painter: PainterArgs => ZIO[EventSource[Painter, PainterEvent], Throwable, Option[Painter]],
    painters: ZStream[EventSource[Painter, PainterEvent], Throwable, (AggregateId, Painter)],
  )

  case class Mutations(
    createPainter: CreatePainterArgs => ZIO[EventSource[Painter, PainterEvent], Throwable, (UUID, Painter)],
    addPaintings: AddPaintingsArgs => ZIO[EventSource[Painter, PainterEvent], Throwable, Painter],
  )

  case class PainterArgs(uuid: AggregateId)
  case class CreatePainterArgs(name: Name, paintings: List[Painting])
  case class AddPaintingsArgs(uuid: AggregateId, paintings: List[Painting])

  private val queries =
    Queries(
      painters readOption _.uuid,
      painters.readAll,
    )

  private val mutations =
    Mutations(
      args => createPainter(args.name, args.paintings.toSet),
      args => addPaintings(args.uuid, args.paintings.toSet),
    )

  object PaintersApi extends GenericSchema[EventSource[Painter, PainterEvent]] {
    val graphql: GraphQL[EventSource[Painter, PainterEvent]] =
      graphQL(
        RootResolver(
          queries,
          mutations,
        )
      )
  }

  private val fullLayer = journal.inMemory[PainterEvent].layer >>> EventSource.live(reduce)

}
