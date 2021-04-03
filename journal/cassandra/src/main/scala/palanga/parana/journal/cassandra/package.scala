package palanga.parana.journal

import palanga.parana.journal.cassandra.CassandraJournal.Codec
import palanga.zio.cassandra.{ CassandraException, ZCqlSession }
import zio.{ Tag, ZLayer }

package object cassandra {

  def live[Ev](implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] =
    CassandraJournal.layer[Ev](shouldCreateTable = false)

  def test[Ev](implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] =
    CassandraJournal.layer[Ev](shouldCreateTable = true)

}
