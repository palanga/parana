package palanga.zio.eventsourcing.journal

import palanga.zio.cassandra.{ CassandraException, ZCqlSession }
import palanga.zio.eventsourcing.journal.cassandra.CassandraJournal.Codec
import zio.{ Tag, ZLayer }

package object cassandra {

  def live[Ev](implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] =
    CassandraJournal.layer[Ev](shouldCreateTable = false)

  def test[Ev](implicit codec: Codec[Ev], etag: Tag[Ev]): ZLayer[ZCqlSession, CassandraException, Journal[Ev]] =
    CassandraJournal.layer[Ev](shouldCreateTable = true)

}
