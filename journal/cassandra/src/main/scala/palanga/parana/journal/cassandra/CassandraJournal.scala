package palanga.parana.journal.cassandra

import com.datastax.oss.driver.api.core.uuid.Uuids
import palanga.parana.AggregateId
import palanga.parana.journal.Journal
import palanga.parana.journal.Journal.Service
import palanga.parana.journal.cassandra.CassandraJournal.Codec
import palanga.zio.cassandra.ZStatement.StringOps
import palanga.zio.cassandra.session.ZCqlSession
import palanga.zio.cassandra.{ CassandraException, ZCqlSession }
import zio._
import zio.stream.ZStream

import java.util.UUID

object CassandraJournal {

  case class Codec[T](encode: T => String, decode: String => Either[Throwable, T])

  def make[Ev](
    shouldCreateTable: Boolean = false
  )(implicit codec: Codec[Ev], etag: Tag[Ev]): ZIO[ZCqlSession, CassandraException, Journal.Service[Ev]] = {

    val tableName = etag.tag.longName.replace('.', '_')

    val createTable =
      s"""
         |CREATE TABLE IF NOT EXISTS $tableName (
         |  id       uuid,
         |  event_id timeuuid,
         |  event    text,
         |  PRIMARY KEY (id, event_id)
         |);
         |""".stripMargin.toStatement

    ZIO.environment[ZCqlSession].flatMap { hasSession =>
      val session = hasSession.get
      session
        .execute(createTable)
        .when(shouldCreateTable)
        .as(new CassandraJournal[Ev](session, tableName))
    }

  }

  def layer[Ev](
    shouldCreateTable: Boolean = false
  )(implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] =
    make(shouldCreateTable).toLayer

}

final private[parana] class CassandraJournal[Ev](
  private val session: ZCqlSession.Service,
  private val tableName: String,
)(implicit codec: Codec[Ev], etag: Tag[Ev])
    extends Service[Ev] {

  private val selectStatement =
    s"SELECT event FROM $tableName WHERE id=?;".toStatement

  private val insertStatement =
    s"INSERT INTO $tableName (id, event_id, event) VALUES (?,?,?);".toStatement

  private val selectAllIds =
    s"SELECT DISTINCT id FROM $tableName;".toStatement.decode(_.getUuid("id"))

  // TODO decode should be safe in zio-cassandra
  override def read(id: UUID): ZStream[Any, CassandraException, Ev] =
    session
      .stream(selectStatement.bind(id).decode(row => codec.decode(row.getString("event")).fold(throw _, identity)))
      .flattenChunks

  override def write(id: AggregateId, event: Ev): ZIO[Any, CassandraException, (AggregateId, Ev)] =
    session
      .execute(insertStatement.bind(id, Uuids.timeBased(), codec.encode(event)))
      .as(id -> event)

  override def allIds: ZStream[Any, CassandraException, AggregateId]                              =
    session
      .stream(selectAllIds)
      .flattenChunks

}
