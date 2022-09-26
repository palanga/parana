package palanga.examples.orders

import palanga.parana.eventsource.*
import palanga.parana.eventsource.local.*
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
      _      <- orders.of(SOME_UUID).get.map(_.total).debug
      _      <- orders.of(SOME_UUID).ask(Command.Pay(10)).debug
      _      <- orders.of(SOME_UUID).get.debug
    yield ()
