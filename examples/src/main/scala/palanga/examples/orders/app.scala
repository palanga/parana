package palanga.examples.orders

import palanga.examples.eventsource.*
import palanga.examples.eventsource_local.*
import commands.*
import events.*
import eventsourcing.*
import model.*
import zio.*

object app extends ZIOAppDefault:

  override def run = app.provide(EventSourceLocal.makeLayer(initCommand, applyCommand))

  private val app =
    for
      orders <- ZIO.service[EventSource[Order, Command, Event]]
      _      <- orders.empty.ask(Command.AddItems(Item("hueso", 10) :: Nil)).debug
      _      <- orders.of("entity uuid").get.map(_.total).debug
      _      <- orders.of("entity uuid").ask(Command.Pay(10)).debug
      _      <- orders.of("entity uuid").get.debug
    yield ()
