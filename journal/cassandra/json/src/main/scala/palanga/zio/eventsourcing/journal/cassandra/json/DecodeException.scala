package palanga.zio.eventsourcing.journal.cassandra.json

case class DecodeException(cause: String) extends Exception(cause)
