package palanga.examples.orders

import model.*

object events:
  enum Event:
    case ItemsAdded(items: List[Item])
    case ItemsRemoved(items: List[Item])
    case Paid(amount: Price)
    case Delivered(to: String)
    case Closed
