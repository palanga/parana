package palanga.examples.orders

import commands.*
import events.*
import model.*
import zio.*

object eventsourcing:

  val initCommand: PartialFunction[Command, (Order, List[Event])] =
    case Command.AddItems(items) =>
      Order(Nil, Status.Opened).addItems(items).getOrElse(???) -> List(Event.ItemsAdded(items))

  def applyCommand(order: Order, cmd: Command): Task[(Order, List[Event])] =
    ZIO
      .fromEither(commandToMethod(cmd)(order))
      .map(_ -> (commandToEvent(cmd) :: Nil))
      .mapError(e => new Exception(e.toString, e))

  private def commandToEvent(cmd: Command): Event = cmd match
    case Command.AddItems(items)    => Event.ItemsAdded(items)
    case Command.RemoveItems(items) => Event.ItemsRemoved(items)
    case Command.Pay(amount)        => Event.Paid(amount)
    case Command.Deliver(to)        => Event.Delivered(to)
    case Command.Close              => Event.Closed

  private def commandToMethod(cmd: Command): Order => Either[Error, Order] = order =>
    cmd match
      case Command.AddItems(items)    => order.addItems(items)
      case Command.RemoveItems(items) => order.removeItems(items)
      case Command.Pay(amount)        => order.pay(amount)
      case Command.Deliver(to)        => order.deliver(to)
      case Command.Close              => order.close
