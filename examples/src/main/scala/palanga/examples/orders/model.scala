package palanga.examples.orders

object model:

  case class Order(items: List[LineItem], status: Status):

    def total: Price = items.map(_.total).sum

    def addItems(items: List[LineItem]): Either[Error, Order] =
      if this.isOpen
      then Right(this.copy(items = items ++ this.items))
      else Left(Error.IllegalState)

    def removeItems(items: List[LineItem]): Either[Error, Order] =
      if this.isOpen
      then Right(this.copy(items = this.items.diff(items)))
      else Left(Error.IllegalState)

    def pay(amount: Price): Either[Error, Order] =
      for
        _ <- validate(canTransitionTo(Status.Paid))(Error.IllegalStatusTransition)
        _ <- validate(amount >= total)(Error.InsufficientFunds)
      yield copy(status = Status.Paid)

    def ship(to: Address): Either[Error, Order] =
      for _ <- validate(canTransitionTo(Status.Shipped(to)))(Error.IllegalStatusTransition)
      yield copy(status = Status.Shipped(to))

    def close: Either[Error, Order] = Right(copy(status = Status.Closed))

    private def isOpen = status match
      case Status.Opened => true
      case _             => false

    private def validate(condition: Boolean)(e: Error) = if condition then Right(()) else Left(e)

    private def canTransitionTo(status: Status) =
      status match
        case Status.Closed                                   => true
        case Status.Opened | Status.Paid | Status.Shipped(_) => status.ordinal == this.status.ordinal + 1

  case class LineItem(name: String, unitPrice: Price, amount: Amount):
    def total: Price = unitPrice * amount

  enum Status:
    case Opened
    case Paid
    case Shipped(to: Address)
    case Closed

  // TODO error name when logging
  enum Error extends Throwable:
    case IllegalStatusTransition, InsufficientFunds, IllegalState

  type Price   = BigDecimal
  type Amount  = Int
  type Address = String
