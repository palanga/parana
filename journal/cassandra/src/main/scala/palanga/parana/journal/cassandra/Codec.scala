package palanga.parana.journal.cassandra

case class Codec[T](encode: T => String, decode: String => Either[Throwable, T])
