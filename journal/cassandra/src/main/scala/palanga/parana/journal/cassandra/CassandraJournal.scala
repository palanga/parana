package palanga.parana.journal.cassandra

import com.datastax.oss.driver.api.core.uuid.Uuids
import palanga.parana.Journal
import palanga.parana.types.*
import palanga.zio.cassandra.ZStatement.StringOps
import palanga.zio.cassandra.{ CassandraException, ZCqlSession }
import zio.*
import zio.stream.ZStream
import palanga.parana.journal.cassandra.Codec

import java.util.UUID

def live[Ev](implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] =
  CassandraJournal.layer[Ev](shouldCreateTable = false)

def test[Ev](implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] =
  CassandraJournal.layer[Ev](shouldCreateTable = true)

object CassandraJournal {


  def make[Ev](
    shouldCreateTable: Boolean = false
  )(implicit codec: Codec[Ev], etag: Tag[Ev]): ZIO[ZCqlSession, CassandraException, Journal[Ev]] = {

    def tableName = etag.tag.longName.replace('.', '_').replace('$', '_')

    def createTable =
      s"""
         |CREATE TABLE IF NOT EXISTS $tableName (
         |  id       uuid,
         |  event_id timeuuid,
         |  event    text,
         |  PRIMARY KEY (id, event_id)
         |);
         |""".stripMargin.toStatement

    ZIO.service[ZCqlSession].flatMap { session =>
      session
        .execute(createTable)
        .when(shouldCreateTable)
        .as(CassandraJournal[Ev](session, tableName))
    }

  }

  def layer[Ev](
    shouldCreateTable: Boolean = false
  )(implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] =
    ZLayer.apply(make(shouldCreateTable))

}

final private[parana] class CassandraJournal[Ev](
  private val session: ZCqlSession,
  private val tableName: String,
)(implicit codec: Codec[Ev], etag: Tag[Ev])
    extends Journal[Ev] {

  private val selectStatement =
    s"SELECT event FROM $tableName WHERE id=?;".toStatement

  private val insertStatement =
    s"INSERT INTO $tableName (id, event_id, event) VALUES (?,?,?);".toStatement

  private val selectAllIds =
    s"SELECT DISTINCT id FROM $tableName;".toStatement.decodeAttempt(_.getUuid("id"))

  override def read(id: UUID): ZStream[Any, CassandraException, Ev] =
    session
      .stream(selectStatement.bind(id).decodeAttempt(row => codec.decode(row.getString("event")).fold(throw _, identity)))
      .flattenChunks

  override def write(id: AggregateId, event: Ev): ZIO[Any, CassandraException, (AggregateId, Ev)] =
    session
      .execute(insertStatement.bind(id, Uuids.timeBased(), codec.encode(event))) // TODO time uuids that can be tested
      .as(id -> event)

  override def allIds: ZStream[Any, CassandraException, AggregateId] =
    session
      .stream(selectAllIds)
      .flattenChunks

}
