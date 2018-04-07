package example.actor

import akka.actor.{Actor, ActorRef, Props}
import example.domain.User

object UserUnreadCountActor {
  /**
   * Messages which the corresponding actor will receive.
   */
  sealed trait Message
  object Message {
    case object Increment extends Message
    case object Decrement extends Message
    case class SetUnreadCount(count: Int) extends Message
  }

  /**
   * Use this to create an instance of the corresponding actor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(user: User, batchUpdater: ActorRef): Props =
    Props(new UserUnreadCountActor(user, batchUpdater))
}

class UserUnreadCountActor(user: User, batchUpdater: ActorRef) extends Actor {
  import UserUnreadCountActor._

  var unreadCount: Int = 0

  def receive = {
    case Message.SetUnreadCount(count) =>
      unreadCount = count
      batchUpdater ! ""

    case Message.Increment =>
      unreadCount += 1
      batchUpdater ! ""

    case Message.Decrement =>
      if(unreadCount > 0) unreadCount -= 1
      batchUpdater ! ""
  }
}
