package palanga.parana.eventsource.local

import palanga.parana.eventsource.CoreSpec.*
import palanga.parana.eventsource.CoreSpec.eventsourcing.*
import palanga.parana.eventsource.CoreSpec.events.*
import palanga.parana.journal.*
import zio.test.*

object EventSourceLocalTest extends ZIOSpecDefault:

  override def spec: Spec[Any, Any] = testSuite.provide(groupsLocal, journalInMemory)

  private val groupsLocal     = EventSourceLocal.makeLayer(initCommand, applyCommand, initEvent, applyEvent)
  private val journalInMemory = InMemoryJournal.makeLayer[Event]
