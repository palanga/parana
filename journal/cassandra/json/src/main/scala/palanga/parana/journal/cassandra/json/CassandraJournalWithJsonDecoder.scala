package palanga.parana.journal.cassandra.json

import palanga.parana.journal.*
import palanga.parana.journal.cassandra.*
import palanga.zio.cassandra.*
import zio.*
import zio.json.*

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
