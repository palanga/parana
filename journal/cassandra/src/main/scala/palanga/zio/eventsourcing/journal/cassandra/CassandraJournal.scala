package palanga.zio.eventsourcing.journal.cassandra

import com.datastax.oss.driver.api.core.uuid.Uuids
import palanga.zio.cassandra.ZStatement.StringOps
import palanga.zio.cassandra.{ CassandraException, ZCqlSession }
import palanga.zio.eventsourcing.AggregateId
import palanga.zio.eventsourcing.journal.Journal
import palanga.zio.eventsourcing.journal.cassandra.CassandraJournal.Codec
import zio._
import zio.stream.ZStream

import java.util.UUID

object CassandraJournal {

  case class Codec[T](encode: T => String, decode: String => Either[Throwable, T])

  def layer[Ev](
    shouldCreateTable: Boolean = false
  )(implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] = {

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

    ZLayer.fromServiceM { session =>
      session
        .execute(createTable)
        .when(shouldCreateTable)
        .as(new CassandraJournal[Ev](session, tableName))
    }

  }
}

final private[eventsourcing] class CassandraJournal[Ev](
  private val session: ZCqlSession.Service,
  private val tableName: String,
)(implicit codec: Codec[Ev], etag: Tag[Ev])
    extends Journal.Service[Ev] {

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

  override def write(event: (AggregateId, Ev)): ZIO[Any, CassandraException, (AggregateId, Ev)] =
    session.execute(insertStatement.bind(event._1, Uuids.timeBased(), codec.encode(event._2))).as(event)

  override def allIds: ZStream[Any, CassandraException, AggregateId] =
    session.stream(selectAllIds).flattenChunks

}
