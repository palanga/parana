package palanga.parana

import zio.{ Has, Tag, ZIO }

package object journal {

  type Journal[Ev] = Has[Journal.Service[Ev]]

  def inMemory[Ev](implicit etag: Tag[Ev]): ZIO[Any, Nothing, Journal.Service[Ev]] = InMemoryJournal.make

  def decorator[Ev](implicit etag: Tag[Ev]): JournalDecorator[Ev] = JournalDecorator()

}
