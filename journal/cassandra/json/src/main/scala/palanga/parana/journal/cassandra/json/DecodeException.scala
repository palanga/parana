package palanga.parana.journal.cassandra.json

case class DecodeException(message: String) extends Exception(message) // TODO use zio cassandra decode exception
