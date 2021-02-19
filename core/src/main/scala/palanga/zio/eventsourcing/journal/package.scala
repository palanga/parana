package palanga.zio.eventsourcing

import zio.{ Has, Tag, ZLayer }

package object journal {

  type Journal[Ev] = Has[Journal.Service[Ev]]

  def inMemory[Ev](implicit etag: Tag[Ev]): ZLayer[Any, Nothing, Journal[Ev]] = InMemoryJournal.layer[Ev]

}
