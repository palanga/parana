package palanga.parana.eventsource.local.internal

import palanga.parana.eventsource.*
import palanga.parana.eventsource.local.*
import palanga.parana.journal.*
import zio.*

final case class EventSourceLocalBuilder[R, A, Cmd, Ev] private[parana] (
  private val initCommand: PartialFunction[Cmd, (A, List[Ev])],
  private val applyCommand: (A, Cmd) => Task[(A, List[Ev])],
  private val initEvent: PartialFunction[Ev, A],
  private val applyEvent: (A, Ev) => Either[Throwable, A],
  private val projections: List[(EntityId, Ev) => ZIO[R, Throwable, Unit]] = Nil,
):

  def withProjection[R1](f: (EntityId, Ev) => ZIO[R1, Throwable, Unit]): EventSourceLocalBuilder[R & R1, A, Cmd, Ev] =
    copy(projections = f :: this.projections)

  def make(using Tag[A], Tag[Cmd], Tag[Ev]): ZIO[R & Journal[Ev], Nothing, EventSourceLocal[A, Cmd, Ev]] =
    for
      journal               <- ZIO.service[Journal[Ev]]
      env                   <- ZIO.environment[R]
      projectionsWithEnv     = projections.map(provideToProjection(env))
      journalWithProjections = projectionsWithEnv.foldLeft(journal)(_.withProjection(_))
    yield EventSourceLocal(initCommand, applyCommand, initEvent, applyEvent, journalWithProjections)

  def makeLayer(using Tag[A], Tag[Cmd], Tag[Ev]): ZLayer[R & Journal[Ev], Nothing, EventSource[A, Cmd, Ev]] =
    ZLayer.fromZIO(make)

  private def provideToProjection[R1](env: ZEnvironment[R1])(
    projection: (EntityId, Ev) => ZIO[R1, Throwable, Unit]
  ): (EntityId, Ev) => ZIO[Any, Throwable, Unit] =
    projection(_, _).provideEnvironment(env)
