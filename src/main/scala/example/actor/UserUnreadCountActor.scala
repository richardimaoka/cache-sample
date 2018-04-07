package example.actor

import akka.actor.{Actor, ActorRef, Props}
import example.domain.User

object UserUnreadCountActor {
  /**
   * Messages which TopicEventProcessorActor will receive.
   */
  sealed trait Message
  object Message {
    case object Increment extends Message
    case object Decrement extends Message
    case class SetUnreadCount(count: Int) extends Message
  }

  /**
   * Use this to create an instance of TopicEventProcessorActor.
   * Return an immutable Props instance so that it can be passed around among actors if necessary.
   */
  def props(user: User): Props = Props(new UserUnreadCountActor(user))
}

class UserUnreadCountActor(user: User) extends Actor {
  import UserUnreadCountActor._

  var batchUpdater: ActorRef = self
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
