package palanga.examples.orders

import model.*

object events:
  enum Event:
    case ItemsAdded(items: List[LineItem])
    case ItemsRemoved(items: List[LineItem])
    case Paid(amount: Price)
    case Shipped(to: Address)
    case Cancelled
