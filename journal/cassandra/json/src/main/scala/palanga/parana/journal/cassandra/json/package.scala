package palanga.parana.journal.cassandra

import palanga.parana.journal.Journal
import palanga.parana.journal.cassandra.CassandraJournal.Codec
import palanga.zio.cassandra.{ CassandraException, ZCqlSession }
import zio.json.*
import zio.{ Tag, ZLayer }

package object json {

  def live[Ev](implicit
    encoder: JsonEncoder[Ev],
    decoder: JsonDecoder[Ev],
    etag: Tag[Ev],
  ): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] = {
    implicit val _codec: Codec[Ev] = Codec(_.toJson, _.fromJson[Ev].left.map(DecodeException.apply))
    CassandraJournal.layer[Ev](shouldCreateTable = false)
  }

  def test[Ev](implicit
    encoder: JsonEncoder[Ev],
    decoder: JsonDecoder[Ev],
    etag: Tag[Ev],
  ): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] = {
    implicit val _codec: Codec[Ev] = Codec(_.toJson, _.fromJson[Ev].left.map(DecodeException.apply))
    CassandraJournal.layer[Ev](shouldCreateTable = true)
  }

}
