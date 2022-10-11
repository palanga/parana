package palanga.parana.eventsource.local

import palanga.parana.eventsource.CoreSpec.*
import palanga.parana.eventsource.CoreSpec.eventsourcing.*
import palanga.parana.eventsource.CoreSpec.events.*
import palanga.parana.journal.*
import zio.test.*

object EventSourceLocalTest extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] =
    suite("locally with an in memory journal")(testSuite).provide(groupsLocal, journalInMemory)

  private val groupsLocal     = EventSourceLocal.of(initCommand, applyCommand, initEvent, applyEvent).makeLayer
  private val journalInMemory = InMemoryJournal.makeLayer[Event]
