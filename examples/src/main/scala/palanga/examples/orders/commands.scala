package palanga.examples.orders

import model.*

object commands:
  enum Command:
    case AddItems(items: List[Item])
    case RemoveItems(items: List[Item])
    case Pay(amount: Price)
    case Deliver(to: String)
    case Close
