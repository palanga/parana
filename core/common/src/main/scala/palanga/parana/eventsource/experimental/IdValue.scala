/**
 * pongo esto acá por ahora asi no me olvido
 */

package palanga.parana.eventsource.experimental

import java.util.UUID

class IdValue[Id[_], A](val id: Id[A], val value: A)

opaque type EntityId[A] = UUID

object EntityId:
  /**
   * Version 4, random UUID
   */
  def random[A]: EntityId[A] = UUID.randomUUID()

opaque type EventId[Ev] = UUID

// TODO use version 1, time-based (no MAC address) UUID for event ids
object EventId:
  def randomTimeBased[Ev]: EventId[Ev] = UUID.randomUUID()

extension [Ev](x: EventId[Ev]) def timestamp: Long = ???

object usage extends App:

  val stringId      = EntityId.random[String]
  val stringEventId = EventId.randomTimeBased[String]
  val intId         = EntityId.random[Int]

  val string: IdValue[EntityId, String]     = IdValue(stringId, "palan")
  val stringEvent: IdValue[EventId, String] = IdValue(stringEventId, "palan")
  val int: IdValue[EntityId, Int]           = IdValue(intId, 7)
//  val dontCompile                           = IdValue(intId, "palan")

  val nube: IdValue[EntityId, String] = IdValue(EntityId.random, "nube")

  val stringId2: EntityId[String] = EntityId.random[String]

  stringEvent.id.timestamp
