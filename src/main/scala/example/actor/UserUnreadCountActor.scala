package example.actor

import akka.actor.{Actor, ActorRef}

object UserUnreadCountActor {
  /**
   * Events which the corresponding actor will receive.
   */
  sealed trait Message
  object Message {
    case object Increment extends Message
    case object Decrement extends Message
    case class SetUnreadCount(count: Int) extends Message
  }
}

class UserUnreadCountActor(batchUpdator: ActorRef) extends Actor {
  import UserUnreadCountActor._

  var unreadCount: Int = 0

  def receive = {
    case Message.SetUnreadCount(count) =>
      unreadCount = count
      batchUpdator()

    case Increment =>
      unreadCount += 1
      notifyFireBase()

    case Decrement =>
      if(unreadCount > 0) unreadCount -= 1
      notifyFireBase()
  }
}
