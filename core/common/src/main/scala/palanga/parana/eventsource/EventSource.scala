package palanga.parana.eventsource

import zio.*

import java.util.UUID

// TODO use version 1, time-based (no MAC address) UUID for event ids. class EventWithId[Ev](id: EventId[Ev], value: Ev)
trait EventSource[A, Cmd, Ev]:
  def empty: EmptyEventSourcedEntity[A, Cmd, Ev]
  def of(id: EntityId): EventSourcedEntity[A, Cmd, Ev]

trait EventSourcedEntity[A, Cmd, Ev]:
  def get: Task[A]
  def ask(command: Cmd): Task[(A, List[Ev])]

trait EmptyEventSourcedEntity[A, Cmd, Ev]:
  def ask(command: Cmd): Task[((EntityId, A), List[Ev])]

type EntityId = UUID // TODO use version 4, random UUID opaque type. EntityWithId[A](id: EntityId[A], value: A)

object Ids2:
  class IdValue[A, Id[_]](val value: A, val id: Id[A])

  opaque type EntityId[A] = UUID
  object EntityId:
    def random[A]: EntityId[A] = UUID.randomUUID()

  opaque type EventId[Ev] = UUID
  object EventId:
    def random[Ev]: EventId[Ev] = UUID.randomUUID()

object prueba3:
  import Ids2.*

  val stringId = EntityId.random[String]
  val stringEventId = EventId.random[String]
  val intId = EntityId.random[Int]

  val string: IdValue[String, Ids2.EntityId] = IdValue("palan", stringId)
  val stringEvent: IdValue[String, Ids2.EventId] = IdValue("palan", stringEventId)
  val int = IdValue(7, intId)
  //  val dontCompile = IdValue(intId, "palan")

  val a = IdValue("nube", EntityId.random)

  val stringId2: Ids2.EntityId[String] = EntityId.random[String]

object Ids:
  class IdValue[Id[_], A](val id: Id[A], val value: A)

  opaque type EntityId[A] = UUID
  object EntityId:
    def random[A]: EntityId[A] = UUID.randomUUID()

  opaque type EventId[Ev] = UUID
  object EventId:
    def random[Ev]: EventId[Ev] = UUID.randomUUID()

object prueba2 extends App:
  import Ids.*

  val stringId      = EntityId.random[String]
  val stringEventId = EventId.random[String]
  val intId         = EntityId.random[Int]

  val string: IdValue[Ids.EntityId, String] = IdValue(stringId, "palan")
  val stringEvent: IdValue[Ids.EventId, String] = IdValue(stringEventId, "palan")
  val int                                   = IdValue(intId, 7)
//  val dontCompile = IdValue(intId, "palan")

  val a = IdValue(EntityId.random, "nube")

  val stringId2: Ids.EntityId[String] = EntityId.random[String]

//object prueba extends App:
//  class IdValue[A, I[_] <: Id[?]](val id: I[A], val value: A)
//
//  trait Id[A](self: UUID)
//
//  class EntityId[A](val self: UUID) extends Id[A](self)
//  class EventId[A](val self: UUID)  extends Id[A](self):
//    def timestamp: Int = self.clockSequence()
//
//  val stringId      = EntityId[String](UUID.randomUUID())
//  val stringEventId = EventId[String](UUID.randomUUID())
//  val intId         = EntityId[Int](UUID.randomUUID())
//
//  val string: IdValue[String, EntityId]     = IdValue(stringId, "palan")
//  val stringEvent: IdValue[String, EventId] = IdValue(stringEventId, "palan")
//  // val string = IdValue(intId, "palan") don't compile
//
//  println(string.id.isInstanceOf[stringEvent.id.type]) // false
//  println(string.id.isInstanceOf[stringId.type])       // true
