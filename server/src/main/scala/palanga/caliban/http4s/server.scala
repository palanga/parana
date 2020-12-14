package palanga.caliban.http4s

import caliban.{ GraphQL, Http4sAdapter }
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.blaze.BlazeServerBuilder
import org.http4s.server.middleware.CORS
import zio.interop.catz._
import zio.{ Has, RIO, Runtime, ZEnv, ZIO }

import scala.concurrent.ExecutionContext

object server {

  def run[R <: Has[_]](
    api: GraphQL[R],
    host: String = "localhost",
    port: Int = 8088,
    executionContext: Runtime[_] => ExecutionContext = _.platform.executor.asEC,
  ): ZIO[ZEnv with R, Throwable, Unit] = {
    type RTask[A] = RIO[ZEnv with R, A]
    ZIO
      .runtime[ZEnv with R]
      .flatMap { implicit runtime =>
        for {
          interpreter <- api.interpreter
          _           <- BlazeServerBuilder[RTask](executionContext(runtime))
                 .bindHttp(port, host)
                 .withHttpApp(
                   Router[RTask](
                     "/api/graphql" -> CORS(Http4sAdapter makeHttpService interpreter),
                     "/ws/graphql"  -> CORS(Http4sAdapter makeWebSocketService interpreter),
                   ).orNotFound
                 )
                 .resource
                 .toManaged
                 .useForever
        } yield ()
      }
  }

}
