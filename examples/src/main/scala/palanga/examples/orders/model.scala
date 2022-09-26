package palanga.examples.orders

object model:

  case class Order(items: List[(Item, Amount)], status: Status):
    def addItems(items: List[Item]): Either[Error, Order]   = Right(this.copy(List(items.head -> items.size))) // TODO
    def removeItems(item: List[Item]): Either[Error, Order] = ???
    def total: Price                                        = items.map((item, amount) => item.price * amount).sum

    def pay(amount: Price): Either[Error, Order] =
      val newStatus = status match
        case Status.Paid | Status.Done => Left(Error.IllegalStatusTransition)
        case Status.Opened             => if amount == total then Right(Status.Paid) else Left(Error.InsufficientFunds)
        case Status.Delivered          => if amount == total then Right(Status.Done) else Left(Error.InsufficientFunds)

      newStatus.map(newStatus => this.copy(status = newStatus))

    def deliver(to: String): Either[Error, Order] = ???

    def close: Either[Error, Order] = ???

  case class Item(name: String, price: Price)

  enum Status:
    case Opened, Paid, Delivered, Done

  enum Error extends Throwable:
    case IllegalState, InsufficientFunds, IllegalStatusTransition

  type Price  = BigDecimal
  type Amount = Int
