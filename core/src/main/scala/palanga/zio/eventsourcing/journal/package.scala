package palanga.zio.eventsourcing

import zio.{ Has, Tag }

package object journal {

  type Journal[Ev] = Has[Journal.Service[Ev]]

  def inMemory[Ev](implicit etag: Tag[Ev]): JournalBuilder[Ev] = JournalBuilder(InMemoryJournal.make)

}
