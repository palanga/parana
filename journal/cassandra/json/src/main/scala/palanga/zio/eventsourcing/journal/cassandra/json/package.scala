package palanga.zio.eventsourcing.journal.cassandra

import palanga.zio.cassandra.{ CassandraException, ZCqlSession }
import palanga.zio.eventsourcing.journal.Journal
import palanga.zio.eventsourcing.journal.cassandra.CassandraJournal.Codec
import zio.json._
import zio.{ Tag, ZLayer }

package object json {

  def live[Ev](implicit
    encoder: JsonEncoder[Ev],
    decoder: JsonDecoder[Ev],
    etag: Tag[Ev],
  ): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] = {
    implicit val _codec: Codec[Ev] = Codec(_.toJson, _.fromJson[Ev].left.map(DecodeException))
    CassandraJournal.layer[Ev](shouldCreateTable = false)
  }

  def test[Ev](implicit
    encoder: JsonEncoder[Ev],
    decoder: JsonDecoder[Ev],
    etag: Tag[Ev],
  ): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] = {
    implicit val _codec: Codec[Ev] = Codec(_.toJson, _.fromJson[Ev].left.map(DecodeException))
    CassandraJournal.layer[Ev](shouldCreateTable = true)
  }

}
