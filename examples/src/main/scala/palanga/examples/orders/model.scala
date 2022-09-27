package palanga.examples.orders

object model:

  case class Order(items: List[LineItem], status: Status):
    def addItems(items: List[LineItem]): Either[Error, Order]   = Right(this.copy(items = items ++ this.items))
    def removeItems(item: List[LineItem]): Either[Error, Order] = ???
    def total: Price                                            = items.map(_.total).sum

    def pay(amount: Price): Either[Error, Order] =
      for
        _ <- validate(canPay)(Error.IllegalStatusTransition)
        _ <- validate(amount >= total)(Error.InsufficientFunds)
      yield copy(status = Status.Paid)

    def deliver(to: String): Either[Error, Order] = ???

    def close: Either[Error, Order] = ???

    private def validate(condition: Boolean)(e: Error) = if condition then Right(()) else Left(e)

    private def canPay: Boolean = this.status match
      case Status.Opened | Status.Delivered => true
      case Status.Paid | Status.Done        => false

  case class LineItem(name: String, unitPrice: Price, amount: Amount):
    def total: Price = unitPrice * amount

  enum Status:
    case Opened, Paid, Delivered, Done

  // TODO error name when logging
  enum Error extends Throwable:
    case IllegalState, InsufficientFunds, IllegalStatusTransition

  type Price  = BigDecimal
  type Amount = Int
