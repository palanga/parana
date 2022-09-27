package palanga.examples.orders

import model.*

object commands:
  enum Command:
    case AddItems(items: List[LineItem])
    case RemoveItems(items: List[LineItem])
    case Pay(amount: Price)
    case Ship(to: Address)
    case Close
